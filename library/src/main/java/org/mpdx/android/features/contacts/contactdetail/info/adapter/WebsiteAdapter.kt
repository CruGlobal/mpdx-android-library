package org.mpdx.android.features.contacts.contactdetail.info.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import org.mpdx.android.base.widget.recyclerview.UniqueItemDataBindingAdapter
import org.mpdx.android.databinding.ContactDetailInfoWebsiteItemBinding
import org.mpdx.android.features.contacts.model.Website
import org.mpdx.android.features.contacts.viewmodel.WebsiteViewModel

class WebsiteAdapter : UniqueItemDataBindingAdapter<Website, ContactDetailInfoWebsiteItemBinding>() {
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ContactDetailInfoWebsiteItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            item = WebsiteViewModel()
        }

    override fun onBindViewDataBinding(binding: ContactDetailInfoWebsiteItemBinding, position: Int) {
        binding.item?.model = getItem(position)
    }
}
