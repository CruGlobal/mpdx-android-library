package org.mpdx.android.features.contacts.contactdetail.tasks

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import io.huannguyen.swipeablerv.SWItemRemovalListener
import javax.inject.Inject
import org.mpdx.android.ContactViewTasksBinding
import org.mpdx.android.R
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.core.modal.ModalActivity
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.analytics.model.TaskDeletedAction
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivityViewModel
import org.mpdx.android.features.tasks.editor.buildTaskEditorActivityIntent
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.repository.TasksRepository
import org.mpdx.android.features.tasks.taskdetail.TaskDetailActivity
import org.mpdx.android.features.tasks.tasklist.CurrentTasksFragment
import org.mpdx.android.features.tasks.tasklist.TasksHistoryFragment
import org.mpdx.android.utils.StringResolver
import org.mpdx.android.utils.minusAssign
import org.mpdx.android.utils.plusAssign

@AndroidEntryPoint
class ContactDetailTaskFragment @Inject constructor() :
    DataBindingFragment<ContactViewTasksBinding>(), ContactTasksViewListener, SWItemRemovalListener<Task?> {
    @Inject
    lateinit var tasksRepository: TasksRepository
    @Inject
    lateinit var stringResolver: StringResolver
    @Inject
    lateinit var appPrefs: AppPrefs

    // region Lifecycle
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?): ContactViewTasksBinding {
        return ContactViewTasksBinding.inflate(inflater, container, false).also { bind ->
            bind.listener = this
            bind.tasks = dataModel.tasks
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }
    // endregion Lifecycle

    private val dataModel: ContactDetailActivityViewModel by activityViewModels()

    // region Tasks List
    val adapter by lazy {
        ContactTasksAdapter(this, stringResolver).also { adapter ->
            dataModel.tasks.observe(this, adapter)
            dataModel.contact.observe(this) { adapter.updateName(it?.name) }
        }
    }

    private fun setupRecyclerView() {
        (binding.contactTasksRecycler.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        binding.contactTasksRecycler.setHasFixedSize(true)
        binding.contactTasksRecycler.setupSwipeToDismiss(adapter, ItemTouchHelper.LEFT)
        adapter.setItemRemovalListener(this)
        binding.contactTasksRecycler.adapter = adapter
    }
    // endregion Tasks List

    // region View Listeners
    override fun toTaskDetail(taskId: String?, activityType: String?) {
        val intent = Intent(context, TaskDetailActivity::class.java)
        intent.putExtra(CurrentTasksFragment.TASK_ID_KEY, taskId)
        intent.putExtra(CurrentTasksFragment.TASK_ACTIVITY_TYPE_KEY, activityType)
        startActivity(intent)
    }

    override fun onTaskDeleted(response: Task?) {
        Toast.makeText(context, R.string.task_deleted, Toast.LENGTH_SHORT).show()
        response?.id?.let { eventBus.post(TaskDeletedAction) }
    }

    override fun onTaskCompleted(taskId: String?) {
        startActivity(context?.buildTaskEditorActivityIntent(taskId = taskId, isFinishTask = true))
    }

    override fun onTaskHistoryClicked() {
        ModalActivity.launchActivity(requireActivity(), TasksHistoryFragment.newInstance(dataModel.contact.value?.id))
    }
    // endregion View Listeners

    // region Item Removal Listeners
    override fun onItemTemporarilyRemoved(item: Task?, position: Int) {
        dataModel.hiddenTasks += setOf(item?.id)
    }

    override fun onItemPermanentlyRemoved(item: Task?) {
        tasksRepository.deleteTask(item?.id)
        dataModel.hiddenTasks -= setOf(item?.id)
    }

    override fun onItemAddedBack(item: Task?, position: Int) {
        dataModel.hiddenTasks -= setOf(item?.id)
    }
    // endregion Item Removal Listeners
}
