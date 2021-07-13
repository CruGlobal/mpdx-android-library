package org.mpdx.android.features.tasks.tasklist

import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Sort
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.tasks.model.TaskFields
import org.mpdx.android.features.tasks.realm.completed
import org.mpdx.android.features.tasks.realm.forContact
import org.mpdx.android.features.tasks.realm.getTasks
import org.mpdx.android.features.tasks.sync.TasksSyncService

@HiltViewModel
class TasksHistoryFragmentViewModel @Inject constructor(
    appPrefs: AppPrefs,
    private val tasksSyncService: TasksSyncService
) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData
    val contactId = MutableLiveData<String?>()

    val tasks by lazy {
        accountListId.switchCombineWith(contactId) { accountListId, contactId ->
            realm.getTasks(accountListId)
                .apply {
                    if (contactId != null) forContact(
                        contactId
                    )
                }
                .completed(true)
                .sort(TaskFields.COMPLETED_AT_VALUE, Sort.DESCENDING)
                .asLiveData()
        }
    }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncContactTasks() }
        contactId.observe(this) { syncContactTasks() }
    }

    fun syncContactTasks(force: Boolean = false) {
        val accountListId = accountListId.value ?: return
        val contactId = contactId.value ?: return
        syncTracker.runSyncTask(tasksSyncService.syncTasksForContact(accountListId, contactId, force))
    }

    fun syncPageOfTasks(page: Int, force: Boolean = false): Boolean {
        val accountListId = accountListId.value ?: return false
        syncTracker.runSyncTasks(
            tasksSyncService.syncCompletedTasks(accountListId, page, force),
            tasksSyncService.syncDeletedTasks(force && page == 1)
        )
        return true
    }
    // endregion Sync logic
}
