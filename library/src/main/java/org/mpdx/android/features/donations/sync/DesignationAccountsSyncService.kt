package org.mpdx.android.features.donations.sync

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
import org.mpdx.android.features.donations.api.DonationsApi
import org.mpdx.android.features.donations.realm.getDesignationAccounts
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "DesignationAccountsSync"

private const val SUBTYPE_DESIGNATION_ACCOUNTS = "designation_accounts"

private const val SYNC_KEY_DESIGNATION_ACCOUNTS = "designation_accounts"

private const val STALE_DURATION_DESIGNATION_ACCOUNTS = DAY_IN_MS

@Singleton
class DesignationAccountsSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val donationsApi: DonationsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_DESIGNATION_ACCOUNTS -> syncDesignationAccounts(args)
        }
    }

    // region DesignationAccounts sync
    private val designationAccountsMutex = MutexMap()
    private suspend fun syncDesignationAccounts(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return
        designationAccountsMutex.withLock(accountListId) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_DESIGNATION_ACCOUNTS, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_DESIGNATION_ACCOUNTS, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                val responses = fetchPages { page ->
                    donationsApi.getDesignationAccounts(accountListId, JsonApiParams().page(page))
                }

                realmTransaction {
                    val accountList = getAccountList(accountListId).findFirst().orPlaceholder(accountListId)
                    saveInRealm(
                        responses.aggregateData().onEach { it.accountList = accountList },
                        getDesignationAccounts(accountListId).asExisting(),
                        deleteOrphanedExistingItems = !responses.hasErrors()
                    )
                    if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the designation accounts from the API")
            }
        }
    }

    fun syncDesignationAccounts(accountListId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(SUBTYPE_DESIGNATION_ACCOUNTS, force)
    // endregion DesignationAccounts sync
}
