package org.mpdx.android.base.sync

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.mpdx.android.core.realm.RealmManager

@Singleton
class SyncDispatcher @Inject constructor(private val realmManager: RealmManager) : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext get() = Dispatchers.Default + job

    private val syncServices = ConcurrentHashMap<String, BaseSyncService>()
    fun registerSyncService(type: String, service: BaseSyncService) {
        syncServices.put(type, service)
            ?.let { throw IllegalStateException("$type already registered in the SyncManager") }
    }

    internal suspend fun runSyncTask(task: SyncTask) {
        try {
            // don't run the sync task if realm is currently locked
            if (!realmManager.isUnlocked) return

            syncServices[task.type]?.sync(task.args)
                ?: throw IllegalStateException("Task type '${task.type}' is not registered")
        } finally {
            // run any onComplete closures
            task.onComplete.forEach { it() }
        }
    }

    internal fun runSyncTaskAsync(task: SyncTask) = async { runSyncTask(task) }
    internal fun launchSyncTask(task: SyncTask) = launch { runSyncTask(task) }
}
