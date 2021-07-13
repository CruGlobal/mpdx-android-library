package org.mpdx.android.features.contacts.contacteditor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.ccci.gto.android.common.recyclerview.adapter.SimpleDataBindingAdapter
import org.mpdx.android.BR
import org.mpdx.android.R
import org.mpdx.android.features.contacts.contacteditor.ContactEditorCallbacks
import org.mpdx.android.features.contacts.contacteditor.FormVisibilityViewModel
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel

private const val TYPE_CONTACT_DETAILS = 2
private const val TYPE_COMMITMENT_HEADER = 3
private const val TYPE_COMMITMENT = 4
private const val TYPE_COMMUNICATION_HEADER = 5
private const val TYPE_COMMUNICATION = 6
private const val TYPE_TAGS = 7

internal class ContactDetailsAdapter(
    lifecycleOwner: LifecycleOwner,
    private val callbacks: ContactEditorCallbacks,
    private val contactViewModel: ContactViewModel,
    private val tags: LiveData<List<String>>,
    private val visibilityViewModel: FormVisibilityViewModel
) : SimpleDataBindingAdapter<ViewDataBinding>(lifecycleOwner) {
    private var sections: List<Int> = generateSections()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
        visibilityViewModel.commitment.observe(lifecycleOwner, Observer { updateSections() })
    }

    private fun generateSections() = mutableListOf<Int>().apply {
        add(TYPE_CONTACT_DETAILS)
        add(TYPE_COMMITMENT_HEADER)
        if (visibilityViewModel.commitment.value == true) add(TYPE_COMMITMENT)
        add(TYPE_COMMUNICATION)
        add(TYPE_TAGS)
    }

    private fun updateSections() {
        sections = generateSections()
    }

    override fun getItemCount() = sections.size
    override fun getItemId(position: Int) = sections[position].toLong()
    override fun getItemViewType(position: Int) = sections[position]
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int): ViewDataBinding {
        val layout = when (viewType) {
            TYPE_CONTACT_DETAILS -> R.layout.contacts_editor_section_contact_details
            TYPE_COMMITMENT_HEADER -> R.layout.contacts_editor_section_commitment_header
            TYPE_COMMITMENT -> R.layout.contacts_editor_section_commitment_details
            TYPE_COMMUNICATION_HEADER -> R.layout.contacts_editor_section_communication_header
            TYPE_COMMUNICATION -> R.layout.contacts_editor_section_communication
            TYPE_TAGS -> R.layout.contacts_editor_section_tags
            else -> throw UnsupportedOperationException("Invalid ViewType - How did we get here??")
        }

        return DataBindingUtil.inflate<ViewDataBinding>(LayoutInflater.from(parent.context), layout, parent, false)
            .apply {
                setVariable(BR.callbacks, ObservableField(callbacks))
                setVariable(BR.contact, contactViewModel)
                setVariable(BR.tags, tags)
                setVariable(BR.visibility, visibilityViewModel)
            }
    }
    override fun onBindViewDataBinding(binding: ViewDataBinding, position: Int) = Unit
}
