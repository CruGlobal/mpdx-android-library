package org.mpdx.android.features.dashboard.view.monthly_goal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.databinding.FragmentDashboardMonthlyGoalBinding

@AndroidEntryPoint
class DashboardMonthlyGoalFragment : DataBindingFragment<FragmentDashboardMonthlyGoalBinding>() {
    private val dataModelMonthly: MonthlyGoalViewModel by viewModels()

    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDashboardMonthlyGoalBinding.inflate(inflater, container, false).apply {
            progressGoal = dataModelMonthly
        }
}
