package org.mpdx.android.features.dashboard.view.to_due_this_week

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import androidx.databinding.ObservableField
import org.mpdx.android.ContactItemBinding
import org.mpdx.android.core.modal.ModalActivity
import org.mpdx.android.databinding.ItemDashboardCelebrationBinding
import org.mpdx.android.databinding.ItemDashboardDueThisWeekHeaderBinding
import org.mpdx.android.databinding.ItemDashboardDueThisWeekViewAllBinding
import org.mpdx.android.databinding.ItemTaskDueThisWeekBinding
import org.mpdx.android.features.contacts.ContactClickListener
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.people.PeopleByGroupingFragment
import org.mpdx.android.features.contacts.people.PersonSelectedListener
import org.mpdx.android.features.contacts.repository.ContactsRepository
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel
import org.mpdx.android.features.contacts.viewmodel.PersonViewModel
import org.mpdx.android.features.dashboard.connect.TaskActionTypeGrouping
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.tasklist.TaskDueDateGrouping
import org.mpdx.android.utils.StringResolver

class DashboardDueThisWeekAdapter() : BaseExpandableListAdapter() {
    lateinit var contactsRepository: ContactsRepository
    var tasksThisWeek = emptyList<Task>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var lateCommitments = emptyList<Contact>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var prayers = emptyList<Task>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var celebrations = emptyList<Pair<String, Person>>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    val contactClickListener = ObservableField<ContactClickListener>()
    val personSelectedListener = ObservableField<PersonSelectedListener>()
    var dueThisWeekListener: DueThisWeekListener? = null

    override fun getGroupCount() = Headers.values().size

    override fun getChildrenCount(groupPosition: Int): Int {
        return when (Headers.values()[groupPosition]) {
            Headers.TASK_DUE_THIS_WEEK -> {
                if (tasksThisWeek.isEmpty()) {
                    return 0
                }
                if (tasksThisWeek.size > 3) {
                    return 4
                }
                return tasksThisWeek.size + 1
            }

            Headers.LATE_COMMITMENTS -> {
                return if (lateCommitments.isEmpty()) {
                    0
                } else {
                    if (lateCommitments.size > 3) {
                        return 4
                    }
                    return lateCommitments.size + 1
                }
            }

            Headers.PARTNER_CAPE_PRAYER -> {
                if (prayers.isEmpty()) {
                    return 0
                }
                if (prayers.size > 3) {
                    return 4
                }
                return prayers.size + 1
            }

            Headers.PARTNER_CARE_CELEBRATIONS -> {
                if (celebrations.isEmpty()) {
                    return 0
                }
                if (celebrations.size > 4) {
                    return 5
                }
                return celebrations.size + 1
            }
        }
    }

    override fun getGroup(groupPosition: Int) = Headers.values()[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): Any? {
        return when (Headers.values()[groupPosition]) {
            Headers.TASK_DUE_THIS_WEEK -> {
                return if (childPosition == tasksThisWeek.size) {
                    null
                } else {
                    tasksThisWeek[childPosition]
                }
            }
            Headers.LATE_COMMITMENTS -> {
                return if (childPosition == lateCommitments.size) {
                    null
                } else {
                    lateCommitments[childPosition]
                }
            }
            Headers.PARTNER_CAPE_PRAYER -> {
                return if (childPosition == prayers.size) {
                    null
                } else {
                    prayers[childPosition]
                }
            }
            Headers.PARTNER_CARE_CELEBRATIONS -> {
                return if (childPosition == celebrations.size) {
                    null
                } else {
                    celebrations[childPosition]
                }
            }
        }
    }

    override fun getGroupId(groupPosition: Int) = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int) = (groupPosition * 1000L) + childPosition

    override fun hasStableIds() = false

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val binding = ItemDashboardDueThisWeekHeaderBinding
            .inflate(LayoutInflater.from(parent?.context), parent, false)
        binding.title = Headers.values()[groupPosition].string
        return binding.root
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val count = getChildrenCount(groupPosition)
        return when (Headers.values()[groupPosition]) {
            Headers.TASK_DUE_THIS_WEEK -> {
                if (childPosition == count - 1) {
                    val binding = ItemDashboardDueThisWeekViewAllBinding
                        .inflate(LayoutInflater.from(parent?.context), parent, false)
                    binding.root.setOnClickListener {
                        dueThisWeekListener?.openAllTask(null, TaskDueDateGrouping.DUE_THIS_WEEK)
                    }
                    return binding.root
                }
                val binding = ItemTaskDueThisWeekBinding.inflate(LayoutInflater.from(parent?.context), parent, false)
                binding.resolver = StringResolver(parent?.context)
                val thisWeek = tasksThisWeek[childPosition]
                binding.root.setOnClickListener { dueThisWeekListener?.onTaskClicked(thisWeek) }
                binding.task = thisWeek
                return binding.root
            }
            Headers.LATE_COMMITMENTS -> {
                if (childPosition == count - 1) {
                    val binding = ItemDashboardDueThisWeekViewAllBinding
                        .inflate(LayoutInflater.from(parent?.context), parent, false)
                    binding.root.setOnClickListener { dueThisWeekListener?.openCommittedContacts(lateCommitments) }
                    return binding.root
                }
                val binding = ContactItemBinding.inflate(LayoutInflater.from(parent?.context), parent, false)
                    .also {
                        it.contactClickListener = contactClickListener
                        it.repository = contactsRepository
                        it.disableHeader = false
                        it.contact = ContactViewModel().apply { model = lateCommitments[childPosition] }
                        it.previous = ContactViewModel().apply { model = null }
                    }
                return binding.root
            }
            Headers.PARTNER_CAPE_PRAYER -> {
                if (childPosition == count - 1) {
                    val binding = ItemDashboardDueThisWeekViewAllBinding
                        .inflate(LayoutInflater.from(parent?.context), parent, false)
                    binding.root.setOnClickListener {
                        dueThisWeekListener?.openAllTask(TaskActionTypeGrouping.PRAYER_REQUEST, null)
                    }
                    return binding.root
                }
                val binding = ItemTaskDueThisWeekBinding
                    .inflate(LayoutInflater.from(parent?.context), parent, false)
                binding.resolver = StringResolver(parent?.context)
                val prayer = prayers[childPosition]
                binding.task = prayer
                binding.root.setOnClickListener { dueThisWeekListener?.onTaskClicked(prayer) }
                return binding.root
            }
            Headers.PARTNER_CARE_CELEBRATIONS -> {
                if (childPosition == count - 1) {
                    val binding = ItemDashboardDueThisWeekViewAllBinding
                        .inflate(LayoutInflater.from(parent?.context), parent, false)
                    binding.root.setOnClickListener {
                        (parent?.context as? Activity)?.let {
                            val array: ArrayList<String> = ArrayList()
                            celebrations.forEach { it.second.id?.let { it1 -> array.add(it1) } }
                            ModalActivity.launchActivity(it, PeopleByGroupingFragment(null, array))
                        }
                    }
                    return binding.root
                }
                val binding = ItemDashboardCelebrationBinding
                    .inflate(LayoutInflater.from(parent?.context), parent, false)
                binding.selectedListener = personSelectedListener
                val celebration = celebrations[childPosition]
                binding.isBirthDay = celebration.first == DASHBOARD_BIRTHDAYS
                binding.person = PersonViewModel().apply { model = celebration.second }
                return binding.root
            }
        }
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int) = true
}
