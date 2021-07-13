package org.mpdx.android.features.contacts.contacteditor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView.NO_ID
import org.ccci.gto.android.common.recyclerview.advrecyclerview.adapter.DataBindingExpandableItemAdapter
import org.ccci.gto.android.common.recyclerview.advrecyclerview.adapter.DataBindingExpandableViewHolder
import org.ccci.gto.android.common.support.v4.util.IdUtils
import org.mpdx.android.BR
import org.mpdx.android.R
import org.mpdx.android.databinding.ContactsEditorSectionPersonHeaderBinding
import org.mpdx.android.features.contacts.contacteditor.ContactEditorCallbacks
import org.mpdx.android.features.contacts.contacteditor.FormVisibilityViewModel
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel
import org.mpdx.android.features.contacts.viewmodel.EmailAddressViewModel
import org.mpdx.android.features.contacts.viewmodel.PersonViewModel
import org.mpdx.android.features.contacts.viewmodel.PhoneNumberViewModel

private const val TYPE_PERSON_DETAILS = 7
private const val TYPE_PHONE = 8
private const val TYPE_PHONE_ADD = 9
private const val TYPE_EMAIL = 10
private const val TYPE_EMAIL_ADD = 11
private const val TYPE_PERSON_DELETE = 12

internal class PersonDetailsAdapter(
    lifecycleOwner: LifecycleOwner,
    private val callbacks: ContactEditorCallbacks,
    private val contactViewModel: ContactViewModel,
    private val visibilityViewModel: FormVisibilityViewModel
) : DataBindingExpandableItemAdapter<ContactsEditorSectionPersonHeaderBinding, ViewDataBinding>(lifecycleOwner) {
    var people: List<PersonViewModel>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
        people = contactViewModel.people.viewModels
    }

    override fun getGroupCount() = people?.size ?: 0
    override fun getChildCount(groupPosition: Int) =
        4 + people?.get(groupPosition).let { it.phoneNumberCount + it.emailAddressCount }
    override fun getGroupId(groupPosition: Int) =
        people?.get(groupPosition)?.model?.id?.let { IdUtils.convertId(it) } ?: NO_ID

    override fun getChildItemViewType(groupPosition: Int, childPosition: Int): Int {
        var max = 1
        if (childPosition < max) return TYPE_PERSON_DETAILS
        val person = people?.get(groupPosition)
        max += person.phoneNumberCount
        if (childPosition < max) return TYPE_PHONE
        max += 1
        if (childPosition < max) return TYPE_PHONE_ADD
        max += person.emailAddressCount
        if (childPosition < max) return TYPE_EMAIL
        max += 1
        if (childPosition < max) return TYPE_EMAIL_ADD
        max += 1
        if (childPosition < max) return TYPE_PERSON_DELETE
        throw IllegalArgumentException("childPosition is too large")
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        val person = people?.get(groupPosition)
        return when (val type = getChildItemViewType(groupPosition, childPosition)) {
            TYPE_PHONE -> person?.getPhoneNumber(childPosition)?.model?.id?.let { IdUtils.convertId(it) } ?: NO_ID
            TYPE_EMAIL -> person?.getEmailAddress(childPosition)?.model?.id?.let { IdUtils.convertId(it) } ?: NO_ID
            else -> type * -1L
        }
    }

    // region Lifecycle

    override fun onCreateGroupViewDataBinding(parent: ViewGroup, viewType: Int) =
        ContactsEditorSectionPersonHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            callbacks = ObservableField(this@PersonDetailsAdapter.callbacks)
            contact = contactViewModel
        }

    override fun onCreateChildViewDataBinding(parent: ViewGroup, viewType: Int): ViewDataBinding {
        val layout = when (viewType) {
            TYPE_PERSON_DETAILS -> R.layout.contacts_editor_section_person_details
            TYPE_PHONE -> R.layout.contacts_editor_section_phone
            TYPE_PHONE_ADD -> R.layout.contacts_editor_section_phone_add
            TYPE_EMAIL -> R.layout.contacts_editor_section_email
            TYPE_EMAIL_ADD -> R.layout.contacts_editor_section_email_add
            TYPE_PERSON_DELETE -> R.layout.contacts_editor_section_person_delete
            else -> throw UnsupportedOperationException("Invalid ViewType - How did we get here??")
        }

        return DataBindingUtil.inflate<ViewDataBinding>(LayoutInflater.from(parent.context), layout, parent, false)
            .apply {
                setVariable(BR.callbacks, ObservableField(callbacks))
                setVariable(BR.contact, contactViewModel)
                setVariable(BR.visibility, visibilityViewModel)
            }
    }

    override fun onBindGroupViewDataBinding(
        holder: DataBindingExpandableViewHolder<ContactsEditorSectionPersonHeaderBinding>,
        binding: ContactsEditorSectionPersonHeaderBinding,
        groupPosition: Int,
        viewType: Int
    ) {
        binding.person = people?.get(groupPosition)
        binding.expanded = holder.expandState.isExpanded
    }

    override fun onBindChildViewDataBinding(
        holder: DataBindingExpandableViewHolder<ViewDataBinding>,
        binding: ViewDataBinding,
        groupPosition: Int,
        childPosition: Int,
        viewType: Int
    ) {
        val person = people?.get(groupPosition)
        binding.setVariable(BR.person, person)
        binding.setVariable(BR.email, person?.getEmailAddress(childPosition))
        binding.setVariable(BR.phone, person?.getPhoneNumber(childPosition))
    }

    override fun onCheckCanExpandOrCollapseGroup(
        holder: DataBindingExpandableViewHolder<ContactsEditorSectionPersonHeaderBinding>,
        groupPosition: Int,
        x: Int,
        y: Int,
        expand: Boolean
    ) = false

    // endregion Lifecycle
}

// region Person Model Accessors

private inline val PersonViewModel?.phoneNumberCount get() = this?.phoneNumbers?.viewModels?.size ?: 0
private inline val PersonViewModel?.emailAddressCount get() = this?.emailAddresses?.viewModels?.size ?: 0

private fun PersonViewModel.getPhoneNumber(position: Int): PhoneNumberViewModel? {
    val index = position - 1
    if (index < 0) return null
    val numbers = phoneNumbers.viewModels
    return if (index < numbers.size) numbers[index] else null
}

private fun PersonViewModel.getEmailAddress(position: Int): EmailAddressViewModel? {
    val index = position - 1 - phoneNumberCount - 1
    if (index < 0) return null
    val addresses = emailAddresses.viewModels
    return if (index < addresses.size) addresses[index] else null
}

// endregion Person Model Accessors
