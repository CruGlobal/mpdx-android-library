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
import org.mpdx.android.features.contacts.model.TwitterAccount
import org.mpdx.android.features.contacts.realm.getPerson
import org.mpdx.android.features.contacts.realm.getTwitterAccounts
import org.mpdx.android.features.tasks.sync.SUBTYPE_TASKS_DIRTY
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "TwitterSyncService"

private const val SUBTYPE_TWIITER_ACCOUNTS_DIRTY = "twitter_accounts_dirty"

@Singleton
class TwitterSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val peopleSyncService: PeopleSyncService,
    private val mediaApi: SocialMediaApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_TASKS_DIRTY -> syncDirtyTwitterAccounts(args)
        }
    }

    // region Dirty Twitter Account sync
    private val dirtyTwitterAccountsMutex = Mutex()
    private suspend fun syncDirtyTwitterAccounts(args: Bundle) = dirtyTwitterAccountsMutex.withLock {
        coroutineScope {
            realm { copyFromRealm(getTwitterAccounts().isDirty().findAll()) }
                .forEach { account: TwitterAccount ->
                    launch {
                        if (account.person?.isNew == true) peopleSyncService.syncDirtyPeople().run()
                        when {
                            account.isDeleted -> syncDeletedTwitterAccount(account)
                            account.isNew -> syncNewTwitterAccount(account)
                            account.hasChangedFields -> syncChangedTwitterAccount(account)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewTwitterAccount(account: TwitterAccount) {
        val contactId = account.person?.contact?.id ?: return
        val personId = account.person?.id ?: return
        try {
            account.prepareForApi()
            mediaApi.createTwitterAccount(contactId, personId, account)
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
                .d(e, "Error syncing a new twitter Account to the API")
        }
    }

    private suspend fun syncChangedTwitterAccount(account: TwitterAccount) {
        val contactId = account.person?.contact?.id ?: return
        val personId = account.person?.id ?: return
        val accountId = account.id ?: return
        try {
            mediaApi.updateTwitterAccount(contactId, personId, accountId, createPartialUpdate(account))
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
                .d(e, "Error storing the changed twitter account %s to the API", accountId)
        }
    }

    private suspend fun syncDeletedTwitterAccount(account: TwitterAccount) {
        val contactId = account.person?.contact?.id ?: return
        val personId = account.person?.id ?: return
        val accountId = account.id ?: return
        try {
            mediaApi.deleteTwitterAccount(contactId, personId, accountId)
                .onSuccess(requireBody = false) { realmTransaction { deleteObj(account) } }
                .onError { data ->
                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error deleting the twitter Account %s", accountId)
        }
    }

    fun syncDirtyTwitterAccounts() = Bundle().toSyncTask(SUBTYPE_TWIITER_ACCOUNTS_DIRTY)
    // endregion Dirty Twitter Account sync
}
