package org.mpdx.android.features.contacts.contactdetail.donation

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mpdx.android.ContactViewDonationBinding
import org.mpdx.android.R
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.core.modal.ModalActivity.Companion.launchActivity
import org.mpdx.android.core.modal.ModalActivity.Companion.launchActivityForResult
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivityViewModel
import org.mpdx.android.features.donations.add.AddDonationFragment
import org.mpdx.android.features.donations.list.DonationsFragment
import org.mpdx.android.features.donations.model.Donation

@AndroidEntryPoint
class ContactDetailDontationFragment :
    DataBindingFragment<ContactViewDonationBinding>(),
    ContactDonationAdapterListener {

    private val dataModel: ContactDetailActivityViewModel by activityViewModels()
    private val adapter by lazy {
        ContactDonationAdapter(this).also { adapter ->
            dataModel.lastSixDonations.observe(this) {
                adapter.updateData(it)
            }
        }
    }
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) = ContactViewDonationBinding
        .inflate(inflater, container, false).also {
            it.lifecycleOwner = viewLifecycleOwner
            it.dataModel = dataModel
            it.presenter = this
            it.contactDonationRecycler.setHasFixedSize(true)
            it.contactDonationRecycler.adapter = adapter
        }

    override fun onDonationClicked(donation: Donation) {
        if (donation.id == null) {
            Toast.makeText(context, R.string.contact_donation_error, Toast.LENGTH_SHORT).show()
            return
        }
        val detailsFragment = AddDonationFragment.newInstance(donation.id)
        launchActivityForResult((context as Activity?)!!, detailsFragment, DonationsFragment.REQUEST_EDIT_DONATION)
    }

    override fun onViewMoreDonationsClicked() {
        val contact = dataModel.contact.value
        contact?.id?.let {
            launchActivity((context as Activity), ContactDonationsFragment(it))
            return
        }
        Toast.makeText(context, R.string.contact_donation_error, Toast.LENGTH_SHORT).show()
    }
}
