package org.mpdx.android.features.contacts.contactdetail.info.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import org.mpdx.android.base.widget.recyclerview.UniqueItemDataBindingAdapter
import org.mpdx.android.databinding.ContactDetailInfoLinkedinItemBinding
import org.mpdx.android.features.contacts.model.LinkedInAccount
import org.mpdx.android.features.contacts.viewmodel.LinkedInAccountViewModel

class LinkedInAdapter : UniqueItemDataBindingAdapter<LinkedInAccount, ContactDetailInfoLinkedinItemBinding>() {
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ContactDetailInfoLinkedinItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            item = LinkedInAccountViewModel()
        }

    override fun onBindViewDataBinding(binding: ContactDetailInfoLinkedinItemBinding, position: Int) {
        binding.item?.model = getItem(position)
    }
}
