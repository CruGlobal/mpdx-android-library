package org.mpdx.android.features.appeals.details.given

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.ccci.gto.android.common.recyclerview.util.lifecycleOwner
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.core.modal.ModalActivity
import org.mpdx.android.databinding.AppealsDetailsPageGivenBinding
import org.mpdx.android.databinding.AppealsGivenViewItemBinding
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_APPEALS_GIVEN
import org.mpdx.android.features.appeals.details.AppealDetailsActivityDataModel
import org.mpdx.android.features.donations.DonationClickedListener
import org.mpdx.android.features.donations.add.AddDonationFragment
import org.mpdx.android.features.donations.model.Donation
import org.mpdx.android.features.donations.viewmodel.DonationViewModel

@AndroidEntryPoint
class CommitmentsGivenFragment : DataBindingFragment<AppealsDetailsPageGivenBinding>(), DonationClickedListener {
    // region Lifecycle
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        AppealsDetailsPageGivenBinding.inflate(inflater, container, false)
            .also { it.appeal = dataModel.appeal }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDonationsList()
    }

    override fun onResume() {
        super.onResume()
        eventBus.post(AnalyticsScreenEvent(SCREEN_APPEALS_GIVEN))
    }

    override fun onDonationClicked(donation: Donation?) {
        donation?.id?.let { ModalActivity.launchActivity(requireActivity(), AddDonationFragment.newInstance(it)) }
    }
    // endregion Lifecycle

    private val dataModel: AppealDetailsActivityDataModel by activityViewModels()

    // region Donations List
    private val adapter by lazy {
        DonationsGivenAdapter().also {
            it.listener.set(this)
            dataModel.donationsGiven.observe(this, it)
        }
    }

    private fun setupDonationsList() {
        binding.donations.lifecycleOwner = viewLifecycleOwner
        binding.donations.adapter = adapter
    }
    // endregion Donations List
}

internal class DonationsGivenAdapter : UniqueItemRealmDataBindingAdapter<Donation, AppealsGivenViewItemBinding>() {
    val listener = ObservableField<DonationClickedListener>()

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        AppealsGivenViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            it.listener = listener
            it.donation = DonationViewModel()
        }

    override fun onBindViewDataBinding(binding: AppealsGivenViewItemBinding, position: Int) {
        binding.donation?.model = getItem(position)
    }
}
