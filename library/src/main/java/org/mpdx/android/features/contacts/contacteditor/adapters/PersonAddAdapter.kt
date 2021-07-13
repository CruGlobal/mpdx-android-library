package org.mpdx.android.features.contacts.contacteditor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.lifecycle.LifecycleOwner
import org.ccci.gto.android.common.recyclerview.adapter.SimpleDataBindingAdapter
import org.mpdx.android.databinding.ContactsEditorSectionPersonAddBinding
import org.mpdx.android.features.contacts.contacteditor.ContactEditorCallbacks
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel

internal class PersonAddAdapter(
    lifecycleOwner: LifecycleOwner,
    private val callbacks: ContactEditorCallbacks,
    private val contactViewModel: ContactViewModel
) : SimpleDataBindingAdapter<ContactsEditorSectionPersonAddBinding>(lifecycleOwner) {
    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = 1
    override fun getItemId(position: Int) = 1L
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ContactsEditorSectionPersonAddBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            callbacks = ObservableField(this@PersonAddAdapter.callbacks)
            contact = contactViewModel
        }

    override fun onBindViewDataBinding(binding: ContactsEditorSectionPersonAddBinding, position: Int) = Unit
}
