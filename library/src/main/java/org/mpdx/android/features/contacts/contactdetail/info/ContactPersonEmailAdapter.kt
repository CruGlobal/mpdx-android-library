package org.mpdx.android.features.contacts.contactdetail.info

import android.view.LayoutInflater
import android.view.ViewGroup
import org.mpdx.android.base.widget.recyclerview.UniqueItemDataBindingAdapter
import org.mpdx.android.databinding.ContactDetailInfoEmailItemBinding
import org.mpdx.android.features.contacts.model.EmailAddress
import org.mpdx.android.features.contacts.viewmodel.EmailAddressViewModel

class ContactPersonEmailAdapter(private val listener: EmailSelectedListener) :
    UniqueItemDataBindingAdapter<EmailAddress, ContactDetailInfoEmailItemBinding>() {
    override fun onBindViewDataBinding(binding: ContactDetailInfoEmailItemBinding, position: Int) {
        binding.email?.model = getItem(position)
    }

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int): ContactDetailInfoEmailItemBinding {
        val binding = ContactDetailInfoEmailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.email = EmailAddressViewModel()
        binding.listener = listener
        return binding
    }

    interface EmailSelectedListener {
        fun onEmailSelected(emailAddress: String?)
    }
}
