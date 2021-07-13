package org.mpdx.android.features.contacts.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.mpdx.android.base.api.ApiConstants.ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.contacts.api.ContactsApiConstants.ERROR_INVALID_PERSON_PREFIX
import org.mpdx.android.features.contacts.api.PhoneNumbersApi
import org.mpdx.android.features.contacts.model.PhoneNumber
import org.mpdx.android.features.contacts.realm.getPerson
import org.mpdx.android.features.contacts.realm.getPhoneNumbers
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "PhoneNumberSyncService"

private const val SUBTYPE_PHONE_NUMBERS_DIRTY = "phone_numbers_dirty"

private const val ERROR_BLANK_NUMBER = "Number can't be blank"
private const val ERROR_NUMBER_NOT_FOUND = "Couldn't find PhoneNumber with 'id'="

@Singleton
class PhoneNumberSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val contactsSyncService: ContactsSyncService,
    private val peopleSyncService: PeopleSyncService,
    private val phoneNumbersApi: PhoneNumbersApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_PHONE_NUMBERS_DIRTY -> syncDirtyPhoneNumbers(args)
        }
    }

    // region Dirty Phone Number sync
    private val dirtyPhoneNumbersMutex = Mutex()
    private suspend fun syncDirtyPhoneNumbers(args: Bundle) = dirtyPhoneNumbersMutex.withLock {
        coroutineScope {
            realm { copyFromRealm(getPhoneNumbers(true).isDirty().findAll()) }
                .forEach { number: PhoneNumber ->
                    launch {
                        if (number.person?.isNew == true) peopleSyncService.syncDirtyPeople().run()
                        when {
                            number.isDeleted -> syncDeletedPhoneNumber(number)
                            number.isNew -> syncNewPhoneNumber(number)
                            number.hasChangedFields -> syncChangedPhoneNumber(number)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewPhoneNumber(number: PhoneNumber) {
        val contactId = number.person?.contact?.id ?: return
        val personId = number.person?.id ?: return
        try {
            number.prepareForApi()
            phoneNumbersApi.createPhoneNumber(contactId, personId, number)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(number)
                        val person = getPerson(personId).findFirst()
                        saveInRealm(body.data.onEach { it.person = person })
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // invalid person, try re-syncing the contact
                            code() == 404 && error.detail?.startsWith(ERROR_INVALID_PERSON_PREFIX) == true -> {
                                contactsSyncService.syncContact(contactId, true).run()
                                return@onError true
                            }

                            // blank phone number, let's just delete it since it's invalid
                            code() == 400 && error.detail == ERROR_BLANK_NUMBER -> {
                                realmTransaction { deleteObj(number) }
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unhandled JsonApiError %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error syncing a new phone number to the API")
        }
    }

    private suspend fun syncChangedPhoneNumber(number: PhoneNumber) {
        val contactId = number.person?.contact?.id ?: return
        val personId = number.person?.id ?: return
        val numberId = number.id ?: return
        try {
            phoneNumbersApi.updatePhoneNumber(contactId, personId, numberId, createPartialUpdate(number))
                .onSuccess { body ->
                    realmTransaction {
                        clearChangedFields(number)
                        val person = getPerson(personId).findFirst()
                        saveInRealm(body.data.onEach { it.person = person })
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // invalid updated_in_db_at time
                            code() == 409 && error.detail?.startsWith(ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX) == true -> {
                                contactsSyncService.syncContact(contactId, true).run()
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unhandled JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error storing the changed phone number %s to the API", numberId)
        }
    }

    private suspend fun syncDeletedPhoneNumber(number: PhoneNumber) {
        val contactId = number.person?.contact?.id ?: return
        val personId = number.person?.id ?: return
        val numberId = number.id ?: return
        try {
            phoneNumbersApi.deletePhoneNumber(contactId, personId, numberId)
                .onSuccess(requireBody = false) { realmTransaction { deleteObj(number) } }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // phone number not found
                            code() == 404 && error.detail?.startsWith(ERROR_NUMBER_NOT_FOUND) == true -> {
                                // phone number was already deleted, so let's delete our local copy
                                realmTransaction { deleteObj(number) }
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unhandled JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error deleting a phone number")
        }
    }

    fun syncDirtyPhoneNumbers() = Bundle().toSyncTask(SUBTYPE_PHONE_NUMBERS_DIRTY)
    // endregion Dirty Phone Number sync
}
