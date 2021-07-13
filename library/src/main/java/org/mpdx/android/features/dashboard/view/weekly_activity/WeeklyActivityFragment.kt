package org.mpdx.android.features.dashboard.view.weekly_activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.databinding.FragmentDashboardWeeklyActivityBinding
import org.mpdx.android.features.coaching.CoachingDetailActivity

@AndroidEntryPoint
class WeeklyActivityFragment : DataBindingFragment<FragmentDashboardWeeklyActivityBinding>() {
    val dataModel by viewModels<WeeklyActivityViewModel>()
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDashboardWeeklyActivityBinding.inflate(inflater, container, false).apply {
            viewModel = dataModel
            weeklyActivityDetail.setOnClickListener {
                startActivity(CoachingDetailActivity.getIntent(context, dataModel.accountListId.value))
            }
        }
}
