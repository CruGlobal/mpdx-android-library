package org.mpdx.androids.library.base.sync

import androidx.annotation.AnyThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SyncTracker : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext get() = Dispatchers.Main + job

    var initialSyncFinished = false
        private set
    private val syncsRunning = MutableLiveData(0)
    val isSyncing = syncsRunning.map { it > 0 }.distinctUntilChanged()

    @AnyThread
    fun runSyncTask(task: SyncTask) = runSyncTasks(task)

    @AnyThread
    fun runSyncTasks(vararg tasks: SyncTask) = launch(Dispatchers.Main.immediate) {
        syncsRunning.value = syncsRunning.value!! + 1
        coroutineScope {
            tasks.forEach { launch { it.runTask() } }
        }
        initialSyncFinished = true
        syncsRunning.value = syncsRunning.value!! - 1
    }

    private suspend inline fun SyncTask.runTask() = withContext(Dispatchers.Main.immediate) {
        syncsRunning.value = syncsRunning.value!! + 1
        try {
            // XXX: I might revisit how I run sync tasks
            withContext(Dispatchers.Default) { run() }
        } catch (e: Throwable) {
            Timber.tag("SyncTracker").e(e, "Unhandled error running sync task")
        } finally {
            syncsRunning.value = syncsRunning.value!! - 1
        }
    }
}
