package org.mpdx.android.features.dashboard.view.to_due_this_week

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.core.modal.ModalActivity
import org.mpdx.android.databinding.FragmentDashboardDueThisWeekBinding
import org.mpdx.android.features.contacts.ContactClickListener
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivity
import org.mpdx.android.features.contacts.list.ContactsByGroupingFragment
import org.mpdx.android.features.contacts.list.ContactsGrouping
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.people.PersonSelectedListener
import org.mpdx.android.features.contacts.repository.ContactsRepository
import org.mpdx.android.features.contacts.sync.ContactsSyncService
import org.mpdx.android.features.dashboard.connect.TaskActionTypeGrouping
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.taskdetail.TaskDetailActivity
import org.mpdx.android.features.tasks.tasklist.TaskDueDateGrouping
import org.mpdx.android.features.tasks.tasklist.TasksViewAllFragment

@AndroidEntryPoint
class DashboardDueThisWeekFragment :
    DataBindingFragment<FragmentDashboardDueThisWeekBinding>(),
    ContactClickListener,
    PersonSelectedListener,
    DueThisWeekListener {

    @Inject
    lateinit var contactsSyncService: ContactsSyncService
    val dataModel by viewModels<DashboardDueThisWeekViewModel>()
    val adapter by lazy {
        DashboardDueThisWeekAdapter().also { adapter ->
            dataModel.birthdayAndAnniversaryPeople.observe(this) { adapter.celebrations = it }
            dataModel.lateCommitments.observe(this) { adapter.lateCommitments = it }
            dataModel.taskDueThisWeek.observe(this) { adapter.tasksThisWeek = it }
            dataModel.tasksPrayers.observe(this) { adapter.prayers = it }
            adapter.contactsRepository = ContactsRepository(contactsSyncService)
            adapter.contactClickListener.set(this)
            adapter.personSelectedListener.set(this)
            adapter.dueThisWeekListener = this
        }
    }

    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDashboardDueThisWeekBinding.inflate(inflater, container, false).also {
            it.viewModel = dataModel
            it.toDueWeekAccordionView.setAdapter(adapter)
        }

    override fun onContactClick(contact: Contact?) {
        startActivity(ContactDetailActivity.getIntent(activity, contact?.id))
    }

    override fun onPersonSelected(person: Person?) {
        person?.contact?.id?.let { contactId ->
            activity?.run { startActivity(ContactDetailActivity.getIntent(this, contactId)) }
        }
    }

    override fun onTaskClicked(task: Task?) {
        startActivity(TaskDetailActivity.getIntent(context, task?.id, null))
    }

    override fun openAllTask(type: TaskActionTypeGrouping?, dueDate: TaskDueDateGrouping?) {
        activity?.let {
            ModalActivity.launchActivity(it, TasksViewAllFragment.newInstance(type, dueDate))
        }
    }

    override fun openCommittedContacts(lateCommitments: List<Contact>) {
        val fragment = ContactsByGroupingFragment(
            ContactsGrouping.PARTNERS_ALL_DAYS_LATE,
            *lateCommitments.mapNotNull { it.id }.toTypedArray()
        )
        activity?.let { ModalActivity.launchActivity(it, fragment) }
    }
}

interface DueThisWeekListener {
    fun onTaskClicked(task: Task?)
    fun openAllTask(type: TaskActionTypeGrouping?, dueDate: TaskDueDateGrouping?)
    fun openCommittedContacts(lateCommitments: List<Contact>)
}
