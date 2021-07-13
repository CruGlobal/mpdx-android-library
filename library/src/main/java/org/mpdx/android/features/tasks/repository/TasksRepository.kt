package org.mpdx.android.features.tasks.repository

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import javax.inject.Inject
import javax.inject.Singleton
import org.mpdx.android.features.tasks.realm.getTask
import org.mpdx.android.features.tasks.sync.TasksSyncService
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransactionAsync
import org.threeten.bp.ZonedDateTime

@Singleton
class TasksRepository @Inject constructor(private val tasksSyncService: TasksSyncService) {
    @MainThread
    fun deleteTask(id: String?) {
        realmTransactionAsync(onSuccess = tasksSyncService.syncDirtyTasks()::launch) {
            getTask(id).findFirst()?.isDeleted = true
        }
    }

    @MainThread
    fun completeTask(taskId: String?) {
        realmTransactionAsync(onSuccess = tasksSyncService.syncDirtyTasks()::launch) {
            getTask(taskId).findFirst()?.apply {
                trackingChanges = true
                isCompleted = true
                completedAt = ZonedDateTime.now()
                trackingChanges = false
            }
        }
    }

    @AnyThread
    fun triggerDirtySync() = tasksSyncService.syncDirtyTasks().launch()

    @Deprecated(
        "This should be handled directly with a local Realm database handle",
        ReplaceWith(
            "realm { getTask(id).findFirst()?.copyFromRealm() }",
            "org.mpdx.android.utils.getRealm",
            "org.mpdx.android.features.tasks.realm.getTask",
            "org.mpdx.android.utils.copyFromRealm"
        )
    )
    fun getSingleCachedTask(id: String?) = realm {
        getTask(id).findFirst()?.copyFromRealm()
    }
}
