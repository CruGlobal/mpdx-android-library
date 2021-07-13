package org.mpdx.android.features.appeals.details.committed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.ccci.gto.android.common.recyclerview.util.lifecycleOwner
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.core.modal.ModalActivity
import org.mpdx.android.databinding.AppealsDetailsPageCommittedBinding
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_APPEALS_COMMITTED
import org.mpdx.android.features.appeals.AddCommitmentFragment
import org.mpdx.android.features.appeals.details.AppealDetailsActivityDataModel
import org.mpdx.android.features.appeals.details.PledgeClickedListener
import org.mpdx.android.features.appeals.details.PledgesAdapter
import org.mpdx.android.features.appeals.model.Pledge

@AndroidEntryPoint
class CommittedContactsFragment : DataBindingFragment<AppealsDetailsPageCommittedBinding>(), PledgeClickedListener {
    // region Lifecycle
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        AppealsDetailsPageCommittedBinding.inflate(inflater, container, false)
            .also { it.appeal = dataModel.appeal }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPledgesList()
    }

    override fun onResume() {
        super.onResume()
        eventBus.post(AnalyticsScreenEvent(SCREEN_APPEALS_COMMITTED))
    }

    override fun onPledgeClicked(pledge: Pledge?) {
        pledge?.appeal?.id?.let {
            ModalActivity.launchActivity(requireActivity(), AddCommitmentFragment.newInstance(it, pledge))
        }
    }
    // endregion Lifecycle

    private val dataModel: AppealDetailsActivityDataModel by activityViewModels()

    // region Pledges List
    private val adapter by lazy {
        PledgesAdapter().also {
            it.listener.set(this)
            dataModel.pledgesCommitted.observe(this, it)
        }
    }

    private fun setupPledgesList() {
        binding.pledges.also {
            it.lifecycleOwner = viewLifecycleOwner
            it.adapter = adapter
        }
    }
    // endregion Pledges List
}
