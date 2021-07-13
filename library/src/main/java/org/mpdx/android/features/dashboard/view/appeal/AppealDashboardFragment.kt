package org.mpdx.android.features.dashboard.view.appeal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.ccci.gto.android.common.androidx.fragment.app.findListener
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.databinding.FragmentDashboardAppealBinding
import org.mpdx.android.features.Page
import org.mpdx.android.features.PageLoadListener

@AndroidEntryPoint
class AppealDashboardFragment : DataBindingFragment<FragmentDashboardAppealBinding>() {
    val dataModel by viewModels<AppealDashboardViewModel>()
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDashboardAppealBinding.inflate(inflater, container, false).apply {
            viewModel = dataModel
            dashboardAppealViewAllAppeals.setOnClickListener {
                findListener<PageLoadListener>()?.loadPage(Page.APPEALS)
            }
        }
}
