package org.mpdx.android.features.donations.list

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import org.ccci.gto.android.common.realm.adapter.RealmDataBindingAdapter
import org.mpdx.android.core.modal.ModalActivity
import org.mpdx.android.databinding.ItemDonationBinding
import org.mpdx.android.features.donations.add.AddDonationFragment
import org.mpdx.android.features.donations.model.Donation
import org.mpdx.android.features.donations.viewmodel.DonationViewModel

class DonationsAdapter : RealmDataBindingAdapter<Donation, ItemDonationBinding>() {
    // region Lifecycle
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ItemDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            .also { it.donation = DonationViewModel() }

    override fun onBindViewDataBinding(binding: ItemDonationBinding, position: Int) {
        binding.donation?.model = getItem(position)
        // TODO: this should be handled in data binding
        val donation = getItem(position)
        binding.root.setOnClickListener {
            ModalActivity.launchActivityForResult(
                it.context as Activity,
                AddDonationFragment.newInstance(donation?.id),
                DonationsFragment.REQUEST_EDIT_DONATION
            )
        }
    }
    // endregion Lifecycle
}
