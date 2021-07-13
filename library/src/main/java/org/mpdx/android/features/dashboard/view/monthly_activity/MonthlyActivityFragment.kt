package org.mpdx.android.features.dashboard.view.monthly_activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.databinding.FragmentDashboardMonthlyActivityBinding

@AndroidEntryPoint
class MonthlyActivityFragment : DataBindingFragment<FragmentDashboardMonthlyActivityBinding>() {
    val dataModel by viewModels<MonthlyActivityViewModel>()
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDashboardMonthlyActivityBinding.inflate(inflater, container, false).apply {
            viewModel = dataModel
        }
}
