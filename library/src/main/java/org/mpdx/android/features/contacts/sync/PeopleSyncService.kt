package org.mpdx.android.features.contacts.sync

import android.os.Bundle
import dagger.Lazy
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.mpdx.android.base.api.ApiConstants.ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.contacts.api.ContactsApi
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.features.contacts.realm.getDirtyPeople
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "PeopleSyncService"

private const val SUBTYPE_PEOPLE_DIRTY = "people_dirty"

private const val ERROR_BLANK_FIRST_NAME = "First name can't be blank"

@Singleton
class PeopleSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val contactsSyncService: Lazy<ContactsSyncService>,
    private val contactsApi: ContactsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_PEOPLE_DIRTY -> syncDirtyPeople(args)
        }
    }

    // region Dirty Address sync
    private val dirtyPeopleMutex = Mutex()
    private suspend fun syncDirtyPeople(args: Bundle) = dirtyPeopleMutex.withLock {
        coroutineScope {
            realm { getDirtyPeople().findAll().copyFromRealm() }
                .forEach { person: Person ->
                    launch {
                        if (person.contact?.isNew == true) contactsSyncService.get().syncDirtyContacts().run()
                        when {
                            person.isDeleted -> syncDeletedPerson(person)
                            person.isNew -> syncNewPerson(person)
                            person.hasChangedFields -> syncChangedPerson(person)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewPerson(person: Person) {
        val contactId = person.contact?.id ?: return
        try {
            person.prepareForApi()
            contactsApi.createPersonAsync(contactId, person)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(person)
                        val contact = getContact(contactId).findFirst()
                        saveInRealm(body.data.onEach { it.contact = contact })
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            code() == 400 && error.detail == ERROR_BLANK_FIRST_NAME -> {
                                realmTransaction {
                                    // let's try setting a first name so the user can repair the person later
                                    getManagedVersion(person)?.apply {
                                        trackingChanges = true
                                        firstName = "(Missing)"
                                    }
                                }
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unrecognized JsonApiError %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error syncing a new person to the API")
        }
    }

    private suspend fun syncChangedPerson(person: Person) {
        val contactId = person.contact?.id ?: return
        val personId = person.id ?: return
        try {
            contactsApi.updatePersonAsync(contactId, personId, createPartialUpdate(person))
                .onSuccess { body ->
                    realmTransaction {
                        clearChangedFields(person)
                        val contact = getContact(contactId).findFirst()
                        saveInRealm(body.data.onEach { it.contact = contact })
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // invalid updated_in_db_at time
                            code() == 409 && error.detail?.startsWith(ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX) == true -> {
                                contactsSyncService.get().syncContact(contactId, true).run()
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unhandled JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error storing the changed person %s to the API", personId)
        }
    }

    private suspend fun syncDeletedPerson(person: Person) {
        val contactId = person.contact?.id ?: return
        val personId = person.id ?: return
        try {
            contactsApi.deletePersonAsync(contactId, personId)
                .onSuccess(requireBody = false) { realmTransaction { deleteObj(person) } }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error deleting a person")
        }
    }

    fun syncDirtyPeople() = Bundle().toSyncTask(SUBTYPE_PEOPLE_DIRTY)
    // endregion Dirty Address sync
}
