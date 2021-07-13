package org.mpdx.android.features.dashboard.view.referral

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.ccci.gto.android.common.recyclerview.util.lifecycleOwner
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.core.modal.ModalActivity
import org.mpdx.android.databinding.FragmentDashboardReferralsBinding
import org.mpdx.android.databinding.ItemDashboardReferralContactBinding
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivity
import org.mpdx.android.features.contacts.list.ContactsByGroupingFragment
import org.mpdx.android.features.contacts.list.ContactsGrouping
import org.mpdx.android.features.contacts.model.Contact

@AndroidEntryPoint
class DashboardReferralFragment :
    DataBindingFragment<FragmentDashboardReferralsBinding>(), DashBoardReferralClickListeners {
    val dataModel by viewModels<ReferralsViewModel>()
    val adapter by lazy {
        DashboardReferralContactAdapter(this).also {
            dataModel.firstThreeReferralContacts.observe(viewLifecycleOwner, it)
        }
    }

    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDashboardReferralsBinding.inflate(inflater, container, false).also {
            it.viewModel = dataModel
            it.listener = this
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.referralRecyclerView.also {
            it.lifecycleOwner = viewLifecycleOwner
            it.adapter = adapter
        }
    }

    override fun onContactClicked(contact: Contact?) {
        contact?.id?.let { startActivity(ContactDetailActivity.getIntent(requireActivity(), it)) }
    }

    override fun onAllReferralsClicked() {
        val referralContactIds = dataModel.allReferralContacts.value?.mapNotNull { it.id }?.toTypedArray() ?: return
        val contactsByGroupingFragment = ContactsByGroupingFragment(ContactsGrouping.REFERRALS, *referralContactIds)
        activity?.let { ModalActivity.launchActivity(it, contactsByGroupingFragment) }
    }
}

interface DashBoardReferralClickListeners {
    fun onContactClicked(contact: Contact?)

    fun onAllReferralsClicked()
}

class DashboardReferralContactAdapter(
    val listeners: DashBoardReferralClickListeners
) : UniqueItemRealmDataBindingAdapter<Contact, ItemDashboardReferralContactBinding>() {
    override fun onBindViewDataBinding(binding: ItemDashboardReferralContactBinding, position: Int) {
        binding.contact = getItem(position)
    }

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ItemDashboardReferralContactBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            listener = listeners
        }
}
