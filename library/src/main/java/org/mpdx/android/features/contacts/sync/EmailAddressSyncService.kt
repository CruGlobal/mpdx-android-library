package org.mpdx.android.features.contacts.sync

import android.os.Bundle
import androidx.annotation.VisibleForTesting
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
import org.mpdx.android.features.contacts.api.EmailAddressesApi
import org.mpdx.android.features.contacts.model.EmailAddress
import org.mpdx.android.features.contacts.realm.getEmailAddresses
import org.mpdx.android.features.contacts.realm.getPerson
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.getManagedVersionOrNull
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "EmailAddressSyncService"

@VisibleForTesting
internal const val SUBTYPE_EMAIL_ADDRESSES_DIRTY = "email_addresses_dirty"

private const val ERROR_EMAIL_ALREADY_EXIST = "Email has already been taken"

@Singleton
class EmailAddressSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val contactsSyncService: ContactsSyncService,
    private val peopleSyncService: PeopleSyncService,
    private val emailAddressesApi: EmailAddressesApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_EMAIL_ADDRESSES_DIRTY -> syncDirtyEmailAddresses(args)
        }
    }

    // region Dirty Email Address sync
    private val dirtyEmailAddressesMutex = Mutex()
    private suspend fun syncDirtyEmailAddresses(args: Bundle) = dirtyEmailAddressesMutex.withLock {
        coroutineScope {
            realm { getEmailAddresses(true).isDirty().findAll().copyFromRealm() }
                .forEach { email: EmailAddress ->
                    launch {
                        if (email.person?.isNew == true) peopleSyncService.syncDirtyPeople().run()
                        when {
                            email.isDeleted -> syncDeletedEmailAddress(email)
                            email.isNew -> syncNewEmailAddress(email)
                            email.hasChangedFields -> syncChangedEmailAddress(email)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewEmailAddress(email: EmailAddress) {
        val contactId = email.person?.contact?.id ?: return
        val personId = email.person?.id ?: return
        try {
            email.prepareForApi()
            emailAddressesApi.createEmailAddress(contactId, personId, email)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(email)
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

                            // invalid updated_in_db_at time
                            code() == 409 && error.detail?.startsWith(ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX) == true -> {
                                contactsSyncService.syncContact(contactId, true).run()
                                return@onError true
                            }

                            code() == 400 && error.detail?.startsWith(ERROR_EMAIL_ALREADY_EXIST) == true -> {
                                // TODO: Find a way to notify the user
                                realmTransaction { deleteObj(email) }
                                contactsSyncService.syncContact(contactId, true).run()
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unrecognized JsonApiError %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error syncing a new email address to the API")
        }
    }

    private suspend fun syncChangedEmailAddress(email: EmailAddress) {
        val contactId = email.person?.contact?.id ?: return
        val personId = email.person?.id ?: return
        val emailId = email.id ?: return
        try {
            emailAddressesApi.updateEmailAddress(contactId, personId, emailId, createPartialUpdate(email))
                .onSuccess { body ->
                    realmTransaction {
                        clearChangedFields(email)
                        val person = getPerson(personId).findFirst()
                        saveInRealm(body.data.onEach { it.person = person })
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            code() == 400 && error.detail?.startsWith(ERROR_EMAIL_ALREADY_EXIST) == true -> {
                                // TODO: Find a way to notify the user
                                realmTransaction {
                                    getManagedVersionOrNull(email)?.clearChanged(EmailAddress.JSON_EMAIL)
                                }
                                contactsSyncService.syncContact(contactId, true).run()
                                return@onError true
                            }
                            // invalid updated_in_db_at time
                            code() == 409 && error.detail?.startsWith(ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX) == true -> {
                                contactsSyncService.syncContact(contactId, true).run()
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error storing the changed email address %s to the API", emailId)
        }
    }

    private suspend fun syncDeletedEmailAddress(email: EmailAddress) {
        val contactId = email.person?.contact?.id ?: return
        val personId = email.person?.id ?: return
        val emailId = email.id ?: return
        try {
            emailAddressesApi.deleteEmailAddress(contactId, personId, emailId)
                .onSuccess(requireBody = false) { realmTransaction { deleteObj(email) } }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error deleting an email address")
        }
    }

    fun syncDirtyEmailAddresses() = Bundle().toSyncTask(SUBTYPE_EMAIL_ADDRESSES_DIRTY)
    // endregion Dirty Email Address sync
}
