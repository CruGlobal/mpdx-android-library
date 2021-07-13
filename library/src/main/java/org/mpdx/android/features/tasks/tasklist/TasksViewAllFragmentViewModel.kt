package org.mpdx.android.features.tasks.tasklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import io.realm.Sort
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.dashboard.connect.TaskActionTypeGrouping
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.realm.completed
import org.mpdx.android.features.tasks.realm.getTasks
import org.mpdx.android.features.tasks.realm.hasType
import org.mpdx.android.features.tasks.realm.isDue
import org.mpdx.android.features.tasks.realm.sortByDate
import org.mpdx.android.features.tasks.sync.TasksSyncService

@HiltViewModel
class TasksViewAllFragmentViewModel @Inject constructor(
    appPrefs: AppPrefs,
    private val tasksSyncService: TasksSyncService
) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData

    val taskActionType = MutableLiveData<TaskActionTypeGrouping>()
    val taskDueDate = MutableLiveData<TaskDueDateGrouping>()

    val tasks: LiveData<RealmResults<Task>> by lazy {
        accountListId.switchCombineWith(taskDueDate, taskActionType) { accountListId, dueDate, type ->
            realm.getTasks(accountListId)
                .apply { if (type != null) hasType(type) }
                .apply { if (dueDate != null) isDue(dueDate) }
                .completed(false)
                .sortByDate(Sort.DESCENDING)
                .asLiveData()
        }
    }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val accountListId = accountListId.value ?: return
        syncTracker.runSyncTasks(
            tasksSyncService.syncTasks(accountListId, force),
            tasksSyncService.syncDeletedTasks(force)
        )
    }
    // endregion Sync logic
}
