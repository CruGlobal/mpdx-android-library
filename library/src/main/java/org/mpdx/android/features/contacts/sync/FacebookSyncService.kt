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
import org.mpdx.android.features.contacts.model.FacebookAccount
import org.mpdx.android.features.contacts.realm.getFacebookAccounts
import org.mpdx.android.features.contacts.realm.getPerson
import org.mpdx.android.features.tasks.sync.SUBTYPE_TASKS_DIRTY
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "FacebookSyncService"

private const val SUBTYPE_FACEBOOK_ACCOUNTS_DIRTY = "facebook_accounts_dirty"

@Singleton
class FacebookSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val peopleSyncService: PeopleSyncService,
    private val mediaApi: SocialMediaApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_TASKS_DIRTY -> syncDirtyFacebookAccounts(args)
        }
    }

    // region Dirty Facebook Account sync

    private val dirtyFacebookAccountsMutex = Mutex()
    private suspend fun syncDirtyFacebookAccounts(args: Bundle) = dirtyFacebookAccountsMutex.withLock {
        coroutineScope {
            realm { copyFromRealm(getFacebookAccounts().isDirty().findAll()) }
                .forEach { account: FacebookAccount ->
                    launch {
                        if (account.person?.isNew == true) peopleSyncService.syncDirtyPeople().run()
                        when {
                            account.isDeleted -> syncDeletedFacebookAccount(account)
                            account.isNew -> syncNewFacebookAccount(account)
                            account.hasChangedFields -> syncChangedFacebookAccount(account)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewFacebookAccount(account: FacebookAccount) {
        val contactId = account.person?.contact?.id ?: return
        val personId = account.person?.id ?: return
        try {
            account.prepareForApi()
            mediaApi.createFacebookAccount(contactId, personId, account)
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
            Timber.tag(TAG).d(e, "Error syncing a new facebook Account to the API")
        }
    }

    private suspend fun syncChangedFacebookAccount(account: FacebookAccount) {
        val contactId = account.person?.contact?.id ?: return
        val personId = account.person?.id ?: return
        val accountId = account.id ?: return
        try {
            mediaApi.updateFacebookAccount(contactId, personId, accountId, createPartialUpdate(account))
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
            Timber.tag(TAG).d(e, "Error storing the facebook account %s to the API", accountId)
        }
    }

    private suspend fun syncDeletedFacebookAccount(account: FacebookAccount) {
        val contactId = account.person?.contact?.id ?: return
        val personId = account.person?.id ?: return
        val accountId = account.id ?: return
        try {
            mediaApi.deleteFacebookAccount(contactId, personId, accountId)
                .onSuccess(requireBody = false) { realmTransaction { deleteObj(account) } }
                .onError { data ->
                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error deleting the facebook Account %s", accountId)
        }
    }

    fun syncDirtyFacebookAccounts() = Bundle().toSyncTask(SUBTYPE_FACEBOOK_ACCOUNTS_DIRTY)

    // endregion Dirty Facebook Account sync
}
