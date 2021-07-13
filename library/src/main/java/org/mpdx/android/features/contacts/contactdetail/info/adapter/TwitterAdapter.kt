package org.mpdx.android.features.contacts.contactdetail.info.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import org.mpdx.android.base.widget.recyclerview.UniqueItemDataBindingAdapter
import org.mpdx.android.databinding.ContactDetailInfoTwitterItemBinding
import org.mpdx.android.features.contacts.model.TwitterAccount
import org.mpdx.android.features.contacts.viewmodel.TwitterAccountViewModel

class TwitterAdapter : UniqueItemDataBindingAdapter<TwitterAccount, ContactDetailInfoTwitterItemBinding>() {
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ContactDetailInfoTwitterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            item = TwitterAccountViewModel()
        }

    override fun onBindViewDataBinding(binding: ContactDetailInfoTwitterItemBinding, position: Int) {
        binding.item?.model = getItem(position)
    }
}
