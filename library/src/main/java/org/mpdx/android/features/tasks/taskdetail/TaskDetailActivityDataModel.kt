package org.mpdx.android.features.tasks.taskdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.combineWith
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.orEmpty
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.tasks.model.CommentFields
import org.mpdx.android.features.tasks.realm.getTask
import org.mpdx.android.features.tasks.sync.TasksSyncService

@HiltViewModel
class TaskDetailActivityDataModel @Inject constructor(
    private val tasksSyncService: TasksSyncService
) : RealmViewModel() {
    val taskId = MutableLiveData<String>()

    val task by lazy { taskId.switchMap { realm.getTask(it).firstAsLiveData() } }
    val contact by lazy { task.switchMap { it?.getContacts()?.firstAsLiveData().orEmpty() } }
    val comments by lazy {
        task.switchMap { it?.getComments()?.sort(CommentFields.CREATED_AT)?.asLiveData().orEmpty() }
    }

    val taskNotFound: LiveData<Boolean> by lazy {
        task.combineWith(syncTracker.isSyncing) { task, syncing ->
            task == null && !syncing && syncTracker.initialSyncFinished
        }
    }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        taskId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val taskId = taskId.value ?: return

        syncTracker.runSyncTasks(tasksSyncService.syncTask(taskId, force))
    }
    // endregion Sync logic
}
