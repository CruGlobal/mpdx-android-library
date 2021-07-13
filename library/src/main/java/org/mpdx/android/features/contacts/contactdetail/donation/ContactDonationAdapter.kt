package org.mpdx.android.features.contacts.contactdetail.donation

import android.view.LayoutInflater
import android.view.ViewGroup
import org.mpdx.android.ContactDonationItemBinding
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.features.donations.model.Donation
import org.mpdx.android.features.donations.viewmodel.DonationViewModel

class ContactDonationAdapter internal constructor(private val presenter: ContactDonationAdapterListener) :
    UniqueItemRealmDataBindingAdapter<Donation, ContactDonationItemBinding>() {
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ContactDonationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            it.presenter = presenter
            it.donation = DonationViewModel()
        }

    override fun onBindViewDataBinding(binding: ContactDonationItemBinding, position: Int) {
        binding.donation?.model = getItem(position)
    }
}

interface ContactDonationAdapterListener {
    fun onDonationClicked(donation: Donation)
    fun onViewMoreDonationsClicked()
}
