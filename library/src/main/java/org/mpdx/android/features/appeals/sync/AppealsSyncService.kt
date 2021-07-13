package org.mpdx.android.features.appeals.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.base.TimeConstants
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.api.util.perPage
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.appeals.api.AppealsApi
import org.mpdx.android.features.appeals.realm.getAppeals
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "AppealsSyncService"

private const val SUBTYPE_APPEALS = "appeals"
private const val SUBTYPE_SINGLE_APPEAL = "single_appeal"

private const val SYNC_APPEALS = "appeals"
private const val SYNC_APPEAL = "appeal"

private const val STALE_DURATION_APPEALS = TimeConstants.DAY_IN_MS

@Singleton
class AppealsSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    appealsApi: AppealsApi
) : BaseAppealsSyncService(syncDispatcher, jsonApiConverter, appealsApi) {
    private val appealsMutex = Mutex()
    private val singleAppealMutex = MutexMap()

    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_APPEALS -> syncAppeals(args)
            SUBTYPE_SINGLE_APPEAL -> syncSingleAppeal(args)
        }
    }

    // region Appeals sync
    private suspend fun syncAppeals(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return

        val params = JsonApiParams().perPage(100)

        appealsMutex.withLock {
            val lastSyncTime = getLastSyncTime(SYNC_APPEALS, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_APPEALS, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                val responses = fetchPages { page ->
                    appealsApi.getAppeals(accountListId, JsonApiParams().addAll(params).page(page))
                }

                realmTransaction {
                    saveInRealm(
                        responses.aggregateData(), getAppeals(accountListId).asExisting(),
                        deleteOrphanedExistingItems = !responses.hasErrors()
                    )
                    if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the appeals from the API")
            }
        }
    }

    fun syncAppeals(accountListId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(SUBTYPE_APPEALS, force)
    // endregion Appeals sync

    // region Single Appeal sync
    private suspend fun syncSingleAppeal(args: Bundle) = args.getAppealId()?.let { appealId ->
        singleAppealMutex.withLock(appealId) {
            val lastSyncTime = getLastSyncTime(SYNC_APPEAL, appealId)
                .also { if (!it.needsSync(STALE_DURATION_APPEALS, args.isForced())) return@withLock }

            lastSyncTime.trackSync()
            try {
                appealsApi.getAppeal(appealId)
                    .onSuccess { body ->
                        realmTransaction {
                            saveInRealm(body.data)
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving appeal '%s' from the API", appealId)
            }
        }
    }

    fun syncAppeal(appealId: String, force: Boolean = false) =
        Bundle().putAppealId(appealId).toSyncTask(SUBTYPE_SINGLE_APPEAL, force)
    // endregion Single Appeal sync
}
