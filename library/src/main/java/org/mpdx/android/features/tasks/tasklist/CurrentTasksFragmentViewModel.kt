package org.mpdx.android.features.tasks.tasklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import io.realm.Sort
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.realm.completed
import org.mpdx.android.features.tasks.realm.getTasks
import org.mpdx.android.features.tasks.realm.hasNoDueDate
import org.mpdx.android.features.tasks.realm.isDueToday
import org.mpdx.android.features.tasks.realm.isDueTomorrow
import org.mpdx.android.features.tasks.realm.isOverdue
import org.mpdx.android.features.tasks.realm.isUpcoming
import org.mpdx.android.features.tasks.realm.sortByDate
import org.mpdx.android.features.tasks.sync.TasksSyncService

@HiltViewModel
class CurrentTasksFragmentViewModel @Inject constructor(
    appPrefs: AppPrefs,
    private val tasksSyncService: TasksSyncService
) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData

    val overdueTasks: LiveData<RealmResults<Task>> by lazy {
        accountListId.switchMap {
            realm.getTasks(it).isOverdue().completed(false).sortByDate(Sort.DESCENDING).asLiveData()
        }
    }

    val todayTasks: LiveData<RealmResults<Task>> by lazy {
        accountListId.switchMap { realm.getTasks(it).isDueToday().completed(false).sortByDate().asLiveData() }
    }

    val tomorrowTasks: LiveData<RealmResults<Task>> by lazy {
        accountListId.switchMap { realm.getTasks(it).isDueTomorrow().completed(false).sortByDate().asLiveData() }
    }

    val upcomingTasks: LiveData<RealmResults<Task>> by lazy {
        accountListId.switchMap { realm.getTasks(it).isUpcoming().completed(false).sortByDate().asLiveData() }
    }

    val noDueDateTasks: LiveData<RealmResults<Task>> by lazy {
        accountListId.switchMap { realm.getTasks(it).hasNoDueDate().completed(false).asLiveData() }
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
