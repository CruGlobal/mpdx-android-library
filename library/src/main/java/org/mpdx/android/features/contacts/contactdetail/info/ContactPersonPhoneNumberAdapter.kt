package org.mpdx.android.features.contacts.contactdetail.info

import android.view.LayoutInflater
import android.view.ViewGroup
import org.mpdx.android.base.widget.recyclerview.UniqueItemDataBindingAdapter
import org.mpdx.android.databinding.ContactDetailInfoPhoneItemBinding
import org.mpdx.android.features.contacts.model.PhoneNumber
import org.mpdx.android.features.contacts.viewmodel.PhoneNumberViewModel

class ContactPersonPhoneNumberAdapter(private val listener: PhoneCallListener) :
    UniqueItemDataBindingAdapter<PhoneNumber, ContactDetailInfoPhoneItemBinding>() {
    override fun onBindViewDataBinding(binding: ContactDetailInfoPhoneItemBinding, position: Int) {
        binding.phoneNumber?.model = getItem(position)
    }

    override fun onCreateViewDataBinding(parent: ViewGroup, position: Int): ContactDetailInfoPhoneItemBinding {
        val binding = ContactDetailInfoPhoneItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.phoneNumber = PhoneNumberViewModel()
        binding.listener = listener
        return binding
    }

    interface PhoneCallListener {
        fun onPhoneCallSelected(phoneNumber: PhoneNumber)
    }
}
