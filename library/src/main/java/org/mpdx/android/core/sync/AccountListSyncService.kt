package org.mpdx.android.core.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.base.TimeConstants.DAY_IN_MS
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.model.addUnique
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.core.AccountListApi
import org.mpdx.android.core.realm.forAccountList
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.core.realm.getAccountLists
import org.mpdx.android.core.realm.getUsers
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "AccountListSyncService"

private const val SUBTYPE_ACCOUNT_LISTS = "account_lists"
private const val SUBTYPE_USERS = "users"

private const val SYNC_KEY_ACCOUNT_LISTS = "account_lists"
private const val SYNC_KEY_USERS = "account_list_users"

private const val STALE_DURATION_ACCOUNT_LISTS = DAY_IN_MS
private const val STALE_DURATION_USERS = DAY_IN_MS

@Singleton
class AccountListSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val accountListApi: AccountListApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_ACCOUNT_LISTS -> syncAccountLists(args)
            SUBTYPE_USERS -> syncUsers(args)
        }
    }

    // region AccountLists sync
    private val accountListsMutex = Mutex()
    private suspend fun syncAccountLists(args: Bundle): Unit = accountListsMutex.withLock {
        val lastSyncTime = getLastSyncTime(SYNC_KEY_ACCOUNT_LISTS)
            .also { if (!it.needsSync(STALE_DURATION_ACCOUNT_LISTS, args.isForced())) return }

        lastSyncTime.trackSync()
        try {
            val responses = fetchPages { accountListApi.getAccountLists(JsonApiParams().page(it)) }

            realmTransaction {
                val existing = getAccountLists().asExisting()
                saveInRealm(
                    responses.aggregateData().onEach { it.isUserAccount = true },
                    existing, deleteOrphanedExistingItems = false
                )

                if (!responses.hasErrors()) {
                    // XXX: This will orphan models in Realm, but we don't have a clean way to handle that yet.
                    existing.values.forEach { it.isUserAccount = false }
                    copyToRealmOrUpdate(lastSyncTime)
                }
            }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error retrieving the account lists from the API")
        }
    }

    fun syncAccountLists(force: Boolean = false) = Bundle().toSyncTask(SUBTYPE_ACCOUNT_LISTS, force)
    // endregion AccountLists sync

    // region AccountList Users Sync
    private val usersMutex = MutexMap()
    private suspend fun syncUsers(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return
        usersMutex.withLock(accountListId) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_USERS, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_USERS, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                val responses = fetchPages { accountListApi.getUsers(accountListId, JsonApiParams().page(it)) }

                realmTransaction {
                    val accountList = getAccountList(accountListId).findFirst() ?: return@realmTransaction
                    val existing = getUsers().forAccountList(accountListId).asExisting()

                    saveInRealm(
                        responses.aggregateData().onEach { it.accountLists.addUnique(accountList) },
                        existing, deleteOrphanedExistingItems = false
                    )

                    if (!responses.hasErrors()) {
                        // XXX: This will orphan models in Realm, but we don't have a clean way to handle that yet.
                        existing.values.forEach { it.accountLists.remove(accountList) }
                        copyToRealmOrUpdate(lastSyncTime)
                    }
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the account lists users from the API")
            }
        }
    }

    fun syncUsers(accountListId: String?, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(SUBTYPE_USERS, force)
    // endregion AccountList Users Sync
}
