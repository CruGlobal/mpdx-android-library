package org.mpdx.android.features.contacts.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import org.ccci.gto.android.common.base.TimeConstants.DAY_IN_MS
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.core.model.orPlaceholder
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.core.realm.getDonorAccounts
import org.mpdx.android.features.contacts.api.DonorAccountsApi
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "DonorAccountSyncService"

private const val SYNC_KEY_DONOR_ACCOUNTS = "donor_accounts"

private const val STALE_DURATION_DONOR_ACCOUNTS = DAY_IN_MS

@Singleton
class DonorAccountSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val donorAccountsApi: DonorAccountsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        syncDonorAccounts(args)
    }

    // region DonorAccounts sync
    private val donorAccountsMutex = MutexMap()

    private suspend fun syncDonorAccounts(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return
        donorAccountsMutex.withLock(accountListId) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_DONOR_ACCOUNTS, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_DONOR_ACCOUNTS, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                val responses = fetchPages { page ->
                    donorAccountsApi.getDonorAccounts(accountListId, JsonApiParams().page(page))
                }

                realmTransaction {
                    val accountList = getAccountList(accountListId).findFirst().orPlaceholder(accountListId)
                    val data = responses.aggregateData()
                        .onEach { it.accountList = accountList }

                    saveInRealm(
                        data, getDonorAccounts(accountListId).asExisting(),
                        deleteOrphanedExistingItems = !responses.hasErrors()
                    )
                    if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the donor accounts from the API")
            }
        }
    }

    fun syncDonorAccounts(accountListId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(force = force)
    // endregion DonorAccounts sync
}
