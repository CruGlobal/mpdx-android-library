package org.mpdx.android.features.contacts.contactdetail.info.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import org.mpdx.android.base.widget.recyclerview.UniqueItemDataBindingAdapter
import org.mpdx.android.databinding.ContactDetailInfoFacebookItemBinding
import org.mpdx.android.features.contacts.model.FacebookAccount
import org.mpdx.android.features.contacts.viewmodel.FacebookAccountViewModel

class FacebookAccountAdapter(private val listener: FacebookSelectListener) :
    UniqueItemDataBindingAdapter<FacebookAccount, ContactDetailInfoFacebookItemBinding>() {
    override fun onBindViewDataBinding(binding: ContactDetailInfoFacebookItemBinding, position: Int) {
        binding.item?.model = getItem(position)
    }

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int): ContactDetailInfoFacebookItemBinding {
        val binding = ContactDetailInfoFacebookItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.item = FacebookAccountViewModel()
        binding.listener = listener
        return binding
    }

    interface FacebookSelectListener {
        fun openFacebookMessenger(username: String)
    }
}
