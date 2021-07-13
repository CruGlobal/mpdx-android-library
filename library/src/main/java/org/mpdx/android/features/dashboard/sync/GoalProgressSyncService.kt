package org.mpdx.android.features.dashboard.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import org.ccci.gto.android.common.base.TimeConstants
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.dashboard.api.GoalApi
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "GoalProgressSyncService"

private const val SUBTYPE_GOAL_PROGRESS = "goal_progress"

private const val SYNC_GOAL_PROGRESS = "goal_progress"

private const val STALE_DURATION_GOAL_PROGRESS = 6 * TimeConstants.HOUR_IN_MS

@Singleton
class GoalProgressSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val goalApi: GoalApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_GOAL_PROGRESS -> syncGoalProgress(args)
        }
    }

    // region GoalProgress sync

    private val goalProgressMutex = MutexMap()
    private suspend fun syncGoalProgress(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return

        goalProgressMutex.withLock(accountListId) {
            val lastSyncTime = getLastSyncTime(SYNC_GOAL_PROGRESS, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_GOAL_PROGRESS, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                goalApi.getGoalProgressAsync(accountListId)
                    .onSuccess { body ->
                        realmTransaction {
                            saveInRealm(body.data)
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the goal progress from the API")
            }
        }
    }

    fun syncGoalProgress(accountListId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(SUBTYPE_GOAL_PROGRESS, force)

    // endregion Task Tags sync
}
