package org.mpdx.android.features.dashboard.view.account_balance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.ccci.gto.android.common.androidx.fragment.app.findListener
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.databinding.FragmentDashboardAccountBalanceBinding
import org.mpdx.android.features.Page
import org.mpdx.android.features.PageLoadListener

@AndroidEntryPoint
class AccountBalanceFragment() : DataBindingFragment<FragmentDashboardAccountBalanceBinding>() {

    val dataModel: AccountBalanceViewModel by viewModels()

    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDashboardAccountBalanceBinding.inflate(inflater, container, false).apply {
            viewModel = dataModel
            accountBalanceViewDonations.setOnClickListener {
                findListener<PageLoadListener>()?.loadPage(Page.DONATIONS)
            }
        }
}
