package org.mpdx.android.features.contacts.sync

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import io.realm.Realm
import io.realm.RealmObject
import java.io.IOException
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.base.TimeConstants.HOUR_IN_MS
import org.ccci.gto.android.common.base.TimeConstants.WEEK_IN_MS
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.ApiConstants.ERROR_NO_TIME_INFORMATION
import org.mpdx.android.base.api.ApiConstants.ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_AT
import org.mpdx.android.base.api.util.asApiTimeRange
import org.mpdx.android.base.api.util.filter
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.api.util.perPage
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.core.DeletedRecordsApi
import org.mpdx.android.core.model.DeletedRecord
import org.mpdx.android.features.contacts.api.ContactsApi
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.features.contacts.realm.getContacts
import org.mpdx.android.features.contacts.realm.getDirtyContacts
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import org.mpdx.android.utils.withoutFractionalSeconds
import retrofit2.Response
import timber.log.Timber

private const val TAG = "ContactsSyncService"

private const val SUBTYPE_CONTACT = "contact"
private const val SUBTYPE_CONTACTS = "contacts"
private const val SUBTYPE_CONTACTS_DELETED = "contacts_deleted"
@VisibleForTesting
internal const val SUBTYPE_CONTACTS_DIRTY = "contacts_dirty"

private const val SYNC_KEY_CONTACT = "contact"
private const val SYNC_KEY_CONTACTS = "contacts"
private const val SYNC_KEY_CONTACTS_DELETED = "contacts_deleted"

private const val STALE_DURATION_CONTACT = HOUR_IN_MS
private const val STALE_DURATION_CONTACTS = 6 * HOUR_IN_MS
private const val STALE_DURATION_CONTACTS_FULL = 4 * WEEK_IN_MS
private const val STALE_DURATION_CONTACTS_DELETED = 6 * HOUR_IN_MS

private const val ERROR_BLANK_NAME = "Name can't be blank"
private const val ERROR_CONTACT_NOT_FOUND_PREFIX = "Couldn't find Contact with 'id'="

@Singleton
class ContactsSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val contactsApi: ContactsApi,
    private val deletedRecordsApi: DeletedRecordsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_CONTACTS -> syncContacts(args)
            SUBTYPE_CONTACT -> syncContact(args)
            SUBTYPE_CONTACTS_DELETED -> syncDeletedContacts(args)
            SUBTYPE_CONTACTS_DIRTY -> syncDirtyContacts(args)
        }
    }

    private val baseContactParams = JsonApiParams()
        .include(Contact.JSON_PEOPLE, Contact.JSON_ADDRESSES)
        .include("${Contact.JSON_PEOPLE}.${Person.JSON_EMAIL_ADDRESSES}")
        .include("${Contact.JSON_PEOPLE}.${Person.JSON_PHONE_NUMBERS}")
        .include("${Contact.JSON_PEOPLE}.${Person.JSON_FACEBOOK_ACCOUNT}")
        .include("${Contact.JSON_PEOPLE}.${Person.JSON_LINKEDIN_ACCOUNT}")
        .include("${Contact.JSON_PEOPLE}.${Person.JSON_TWITTER}")
        .include("${Contact.JSON_PEOPLE}.${Person.JSON_WEBSITE}")
        .perPage(100)

    // region Contacts Sync
    private val contactsMutex = MutexMap()

    private suspend fun syncContacts(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return

        contactsMutex.withLock(accountListId) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_CONTACTS, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_CONTACTS, args.isForced())) return }
            val needsFullSync = lastSyncTime.needsFullSync(STALE_DURATION_CONTACTS_FULL, args.isForced())

            // build sync params
            val params = JsonApiParams().addAll(baseContactParams)
            if (!needsFullSync) {
                params.filter(JSON_ATTR_UPDATED_AT, lastSyncTime.getSinceLastSyncRange().asApiTimeRange())
            }

            lastSyncTime.trackSync(fullSync = needsFullSync)
            try {
                val existing =
                    if (needsFullSync) realm { getContacts(accountListId, true).findAll().copyFromRealm().asExisting() }
                    else null

                var hasError = false
                coroutineScope {
                    val channel = Channel<Response<JsonApiObject<Contact>>>(UNLIMITED)
                    launch {
                        fetchPagesInto(channel) {
                            contactsApi.getContacts(accountListId, JsonApiParams().addAll(params).page(it))
                        }
                    }

                    for (response in channel) {
                        response
                            .onSuccess { body -> realmTransaction { saveInRealm(body.data, existing, false) } }
                            .onError {
                                hasError = true
                                return@onError true
                            }
                    }
                }

                if (!hasError) {
                    realmTransaction { copyToRealmOrUpdate(lastSyncTime) }

                    // run individual syncs for orphaned contacts to check if they are deleted or just hidden
                    coroutineScope {
                        existing?.values
                            ?.filterNot { it.isNew }
                            ?.mapNotNull { it.id }
                            ?.forEach { launch { syncContact(it, args.isForced()).run() } }
                    }
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the contacts from the API")
            }
        }
    }

    fun syncContacts(accountListId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(SUBTYPE_CONTACTS, force)
    // endregion Contacts Sync

    // region Single Contact sync
    private val contactMutex = MutexMap()
    private suspend fun syncContact(args: Bundle) {
        val contactId = args.getContactId() ?: return

        contactMutex.withLock(contactId) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_CONTACT, contactId)
                .also { if (!it.needsSync(STALE_DURATION_CONTACT, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                contactsApi.getContact(contactId, baseContactParams)
                    .onSuccess { body ->
                        realmTransaction {
                            saveInRealm(body.data)
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
                    .onError {
                        when (code()) {
                            500 -> true
                            403, 404 -> {
                                realmTransaction { deleteOrphanedItems(getContact(contactId).findAll()) }
                                true
                            }
                            else -> false
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the contact from the API")
            }
        }
    }

    fun syncContact(contactId: String, force: Boolean = false) = Bundle()
        .putContactId(contactId)
        .toSyncTask(SUBTYPE_CONTACT, force)
    // endregion Single Contact sync

    // region Deleted Contacts sync
    private val deletedContactsMutex = Mutex()
    private suspend fun syncDeletedContacts(args: Bundle): Unit = deletedContactsMutex.withLock {
        val lastSyncTime = getLastSyncTime(SYNC_KEY_CONTACTS_DELETED)
            .also { if (!it.needsSync(STALE_DURATION_CONTACTS_DELETED, args.isForced())) return }
        val since = lastSyncTime.getSinceLastSyncRange().start
            .withoutFractionalSeconds()

        val params = JsonApiParams()
            .perPage(500)

        lastSyncTime.trackSync()
        try {
            val responses = fetchPages { page ->
                deletedRecordsApi.getDeletedRecords(
                    DeletedRecord.TYPE_CONTACT, since,
                    JsonApiParams().addAll(params).page(page)
                )
            }

            realmTransaction {
                responses.aggregateData().forEach {
                    deleteOrphanedItems(getContact(it.deletableId).findAll())
                }
                if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
            }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error retrieving the contact deleted records from the API")
        }
    }

    fun syncDeletedContacts(force: Boolean = false) = Bundle().toSyncTask(SUBTYPE_CONTACTS_DELETED, force)
    // endregion Deleted Contacts sync

    // region Dirty Contact sync
    private val dirtyContactsMutex = Mutex()
    private suspend fun syncDirtyContacts(args: Bundle) = dirtyContactsMutex.withLock {
        coroutineScope {
            realm { copyFromRealm(getDirtyContacts().findAll()) }
                .forEach { contact: Contact ->
                    launch {
                        when {
                            contact.isDeleted -> TODO("We don't currently support deleting contacts on the mobile app")
                            contact.isNew -> syncNewContact(contact)
                            contact.hasChangedFields -> syncChangedContact(contact)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewContact(contact: Contact) {
        try {
            contact.createDefaultPerson = realm { getManagedVersion(contact)?.getPeople()?.count() } ?: 0L == 0L
            contact.prepareForApi()
            contactsApi.createContact(baseContactParams, contact)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(contact)
                        saveInRealm(body.data)
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // blank contact name
                            code() == 400 && error.detail == ERROR_BLANK_NAME -> {
                                realmTransaction {
                                    // let's try setting a contact name so the user can repair the contact later
                                    getManagedVersion(contact)?.apply {
                                        trackingChanges = true
                                        name = greeting ?: envelopeGreeting ?: "Unnamed Contact"
                                    }
                                }
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unhandled JsonApiError %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error syncing a new contact to the API")
        }
    }

    private suspend fun syncChangedContact(contact: Contact) {
        val contactId = contact.id ?: return
        try {
            contactsApi.updateContact(contactId, baseContactParams, createPartialUpdate(contact))
                .onSuccess { body ->
                    realmTransaction {
                        clearChangedFields(contact)
                        saveInRealm(body.data)
                    }
                }
                .onError { data ->
                    if (code() == 500 || code() == 502) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // contact not found, refresh from API to see if it still exists
                            code() == 404 && error.detail?.startsWith(ERROR_CONTACT_NOT_FOUND_PREFIX) == true -> {
                                syncContact(contactId, true).run()
                                return@onError true
                            }

                            // invalid updated_in_db_at time
                            code() == 409 && error.detail?.startsWith(ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX) == true -> {
                                syncContact(contactId, true).run()
                                return@onError true
                            }

                            // no time information in ""
                            code() == 400 && error.detail?.equals(ERROR_NO_TIME_INFORMATION) == true -> {
                                if (contact.updatedInDatabaseAt == null) {
                                    realmTransaction { getManagedVersion(contact)?.updatedInDatabaseAt = Date(0) }
                                    syncContact(contactId, true).run()
                                    return@onError true
                                }
                            }
                        }

                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error storing the changed contact %s to the API", contactId)
        }
    }

    fun syncDirtyContacts() = Bundle().toSyncTask(SUBTYPE_CONTACTS_DIRTY)
    // endregion Dirty Contact sync

    // region Realm Functions
    private fun Realm.saveInRealm(
        contacts: Collection<Contact?>,
        existingItems: MutableMap<String?, out RealmObject>? = null,
        deleteOrphanedExistingItems: Boolean = true
    ) {
        contacts.filterNotNull().forEach {
            // save addresses in Realm
            saveInRealm(
                it.apiAddresses.orEmpty(),
                getManagedVersion(it)?.getAddresses(includeDeleted = true)?.asExisting()
            )
            // save people in Realm
            savePeopleInRealm(
                it.apiPeople.orEmpty(),
                getManagedVersion(it)?.getPeople(includeDeleted = true)?.asExisting()
            )
        }
        saveInRealm(contacts as Collection<RealmObject?>, existingItems, deleteOrphanedExistingItems)
    }

    private fun Realm.savePeopleInRealm(
        people: Collection<Person?>,
        existingItems: MutableMap<String?, out RealmObject>? = null,
        deleteOrphanedExistingItems: Boolean = true
    ) {
        people.filterNotNull().forEach {
            // save email addresses in Realm
            saveInRealm(
                it.apiEmailAddresses.orEmpty(),
                getManagedVersion(it)?.getEmailAddresses(includeDeleted = true)?.asExisting()
            )
            // save phone numbers in Realm
            saveInRealm(
                it.apiPhoneNumbers.orEmpty(),
                getManagedVersion(it)?.getPhoneNumbers(includeDeleted = true)?.asExisting()
            )
            // save facebook Accounts in Realm
            saveInRealm(
                it.apiFacebookAccounts.orEmpty(),
                getManagedVersion(it)?.getFacebookAccounts(includeDeleted = true)?.asExisting()
            )
            // save LinkedIn Accounts in Realm
            saveInRealm(
                it.apiLinkedInAccounts.orEmpty(),
                getManagedVersion(it)?.getLinkedInAccounts(includeDeleted = true)?.asExisting()
            )
            // save Twitter Accounts in Realm
            saveInRealm(
                it.apiTwitterAccounts.orEmpty(),
                getManagedVersion(it)?.getTwitterAccounts(includeDeleted = true)?.asExisting()
            )
            // save websites in Realm
            saveInRealm(
                it.apiWebsites.orEmpty(),
                getManagedVersion(it)?.getWebsites(includeDeleted = true)?.asExisting()
            )
        }
        saveInRealm(people, existingItems, deleteOrphanedExistingItems)
    }
    // endregion Realm Functions
}
