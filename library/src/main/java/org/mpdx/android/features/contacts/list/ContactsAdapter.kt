package org.mpdx.android.features.contacts.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ObservableField
import org.mpdx.android.ContactItemBinding
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.features.contacts.ContactClickListener
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.repository.ContactsRepository
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel

class ContactsAdapter(private val contactsRepository: ContactsRepository, private val showHeaders: Boolean = true) :
    UniqueItemRealmDataBindingAdapter<Contact, ContactItemBinding>() {
    val contactClickListener = ObservableField<ContactClickListener>()

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ContactItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            it.contactClickListener = contactClickListener
            it.repository = contactsRepository
            it.disableHeader = !showHeaders
            it.previous = ContactViewModel()
            it.contact = ContactViewModel()
        }

    override fun onBindViewDataBinding(binding: ContactItemBinding, position: Int) {
        binding.contact?.model = getItem(position)
        binding.previous?.model = if (position > 0) getItem(position - 1) else null
    }
}
