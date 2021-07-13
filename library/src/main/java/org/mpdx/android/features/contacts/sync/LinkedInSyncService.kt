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
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.contacts.api.SocialMediaApi
import org.mpdx.android.features.contacts.model.LinkedInAccount
import org.mpdx.android.features.contacts.realm.getLinkedInAccounts
import org.mpdx.android.features.contacts.realm.getPerson
import org.mpdx.android.features.tasks.sync.SUBTYPE_TASKS_DIRTY
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "LinkedInSyncService"

private const val SUBTYPE_LINKEDIN_ACCOUNT_DIRTY = "linkedin_account_dirty"

@Singleton
class LinkedInSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val peopleSyncService: PeopleSyncService,
    private val mediaApi: SocialMediaApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_TASKS_DIRTY -> syncDirtyLinkedInAccounts(args)
        }
    }

    // region Dirty Linkedin Account sync

    private val dirtyLinkedInAccountsMutex = Mutex()
    private suspend fun syncDirtyLinkedInAccounts(args: Bundle) = dirtyLinkedInAccountsMutex.withLock {
        coroutineScope {
            realm { getLinkedInAccounts().isDirty().findAll().copyFromRealm() }
                .forEach { account: LinkedInAccount ->
                    launch {
                        if (account.person?.isNew == true) peopleSyncService.syncDirtyPeople().run()
                        when {
                            account.isDeleted -> syncDeletedLinkedInAccount(account)
                            account.isNew -> syncNewLinkedInAccount(account)
                            account.hasChangedFields -> syncChangedLinkedInAccount(account)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewLinkedInAccount(account: LinkedInAccount) {
        val contactId = account.person?.contact?.id ?: return
        val personId = account.person?.id ?: return
        try {
            account.prepareForApi()
            mediaApi.createLinkedInAccount(contactId, personId, account)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(account)
                        val person = getPerson(personId).findFirst()
                        saveInRealm(body.data.onEach { it.person = person })
                    }
                }
                .onError { data ->
                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unhandled JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error syncing a new LinkedIn Account to the API")
        }
    }

    private suspend fun syncChangedLinkedInAccount(account: LinkedInAccount) {
        val contactId = account.person?.contact?.id ?: return
        val personId = account.person?.id ?: return
        val accountId = account.id ?: return
        try {
            mediaApi.updateLinkedInAccount(contactId, personId, accountId, createPartialUpdate(account))
                .onSuccess { body ->
                    realmTransaction {
                        clearChangedFields(account)
                        val person = getPerson(personId).findFirst()
                        saveInRealm(body.data.onEach { it.person = person })
                    }
                }
                .onError { data ->
                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error storing the changed Linked In Account %s to the API", accountId)
        }
    }

    private suspend fun syncDeletedLinkedInAccount(account: LinkedInAccount) {
        val contactId = account.person?.contact?.id ?: return
        val personId = account.person?.id ?: return
        val accountId = account.id ?: return
        try {
            mediaApi.deleteLinkedInAccount(contactId, personId, accountId)
                .onSuccess(requireBody = false) { realmTransaction { deleteObj(account) } }
                .onError { data ->
                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error deleting an linkedIn Account")
        }
    }

    fun syncDirtyLinkedInAccounts() = Bundle().toSyncTask(SUBTYPE_LINKEDIN_ACCOUNT_DIRTY)

    // endregion Dirty Linkedin Account sync
}
