package org.mpdx.android.features.appeals.sync

import android.os.Bundle
import io.realm.Realm
import io.realm.RealmObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.base.TimeConstants.DAY_IN_MS
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.util.filter
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.api.util.perPage
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.appeals.api.AppealsApi
import org.mpdx.android.features.appeals.api.AskedContactsApi
import org.mpdx.android.features.appeals.model.AskedContact
import org.mpdx.android.features.appeals.realm.getAppeal
import org.mpdx.android.features.appeals.realm.getDirtyAskedContacts
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.JSON_API_TYPE_CONTACT
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "AskedContactSyncService"

private const val SUBTYPE_ASKED_CONTACTS = "asked_contacts"
private const val SUBTYPE_DIRTY_ASKED_CONTACTS = "dirty_asked_contacts"

private const val SYNC_ASKED_CONTACTS = "asked_contacts"

private const val STALE_DURATION_ASKED_CONTACTS = DAY_IN_MS

@Singleton
class AskedContactsSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    appealsApi: AppealsApi,
    private val askedContactsApi: AskedContactsApi
) : BaseAppealsSyncService(syncDispatcher, jsonApiConverter, appealsApi) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_ASKED_CONTACTS -> syncAskedContacts(args)
            SUBTYPE_DIRTY_ASKED_CONTACTS -> syncDirtyAskedContacts(args)
        }
    }

    private val baseParams = JsonApiParams()
        .include(AskedContact.JSON_CONTACT)
        .fields(JSON_API_TYPE_CONTACT, *Contact.JSON_FIELDS_SPARSE)
        .perPage(100)

    // region AskedContacts sync
    private val askedContactsMutex = MutexMap()
    private suspend fun syncAskedContacts(args: Bundle) {
        args.getAppealId()?.let { appealId ->
            askedContactsMutex.withLock(appealId) {
                val lastSyncTime = getLastSyncTime(SYNC_ASKED_CONTACTS, appealId)
                    .also { if (!it.needsSync(STALE_DURATION_ASKED_CONTACTS, args.isForced())) return }

                val params = JsonApiParams().addAll(baseParams)
                    .filter("pledged_to_appeal", "false")

                lastSyncTime.trackSync()
                try {
                    val responses = fetchPages { page ->
                        askedContactsApi.getAskedContacts(appealId, JsonApiParams().addAll(params).page(page))
                    }

                    realmTransaction {
                        val appeal = getAppeal(appealId).findFirst()
                        saveInRealm(
                            responses.aggregateData(), appeal?.getAskedContacts(true)?.asExisting(),
                            deleteOrphanedExistingItems = !responses.hasErrors()
                        )
                        if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
                    }
                } catch (e: IOException) {
                    Timber.tag(TAG)
                        .d(e, "Error retrieving the asked contacts for appeal %s from the API", appealId)
                }
            }
        }
    }

    fun syncAskedContacts(appealId: String, force: Boolean = false) =
        Bundle().putAppealId(appealId).toSyncTask(SUBTYPE_ASKED_CONTACTS, force)
    // endregion AskedContacts sync

    // region Dirty AskedContacts sync
    private val dirtyAskedContactsMutex: Mutex = Mutex()
    private suspend fun syncDirtyAskedContacts(args: Bundle) = dirtyAskedContactsMutex.withLock {
        coroutineScope {
            realm { copyFromRealm(getDirtyAskedContacts().findAll()) }
                .forEach { askedContact ->
                    launch {
                        when {
                            askedContact.isDeleted -> TODO("We currently don't support deleting Appeals AskedContacts")
                            askedContact.isNew -> syncNewAskedContact(askedContact)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewAskedContact(askedContact: AskedContact) {
        val appealId = askedContact.appeal?.id ?: return

        try {
            askedContactsApi.addAskedContact(appealId, askedContact, baseParams)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(askedContact)
                        saveInRealm(body.data)
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            error.detail == "Contact has already been taken" ||
                                error.detail == "Contact is on the Excluded List." -> {
                                realmTransaction { deleteObj(askedContact) }
                                return@onError true
                            }
                            else -> Timber.tag(TAG).e("Unrecognized JsonApiError %s", error.detail)
                        }
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error retrieving the asked contacts for appeal %s from the API", appealId)
        }
    }

    fun syncDirtyAskedContacts() = Bundle().toSyncTask(SUBTYPE_DIRTY_ASKED_CONTACTS)
    // endregion Dirty AskedContacts sync

    // region Realm Functions
    private fun Realm.saveInRealm(
        contacts: Collection<AskedContact?>,
        existingItems: MutableMap<String?, out RealmObject>? = null,
        deleteOrphanedExistingItems: Boolean = true
    ) {
        saveInRealm(
            contacts.onEach {
                it?.contact?.apply {
                    isPlaceholder = true
                    replacePlaceholder = true
                }
            } as Collection<RealmObject?>,
            existingItems,
            deleteOrphanedExistingItems
        )
    }
    // endregion Realm Functions
}
