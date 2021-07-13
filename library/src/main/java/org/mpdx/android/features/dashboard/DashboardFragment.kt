package org.mpdx.android.features.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.viewpager.widget.SwipeRefreshLayoutViewPagerHelper
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.databinding.FragmentDashboardNewBinding
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_DASHBOARD_CONNECT
import org.mpdx.android.features.appeals.sync.AppealsSyncService
import org.mpdx.android.features.base.fragments.BindingFragment
import org.mpdx.android.features.dashboard.sync.GoalProgressSyncService
import org.mpdx.android.features.donations.sync.DonationsSyncService
import org.mpdx.android.features.tasks.sync.TasksSyncService
import org.threeten.bp.YearMonth

// TODO change to new way of setting args
fun createDashboardFragment(id: String?, deepLinkTime: Long) = DashboardFragment().apply {
    setDeepLinkId(id, deepLinkTime)
}

@AndroidEntryPoint
class DashboardFragment : BindingFragment<FragmentDashboardNewBinding>() {
    @Inject
    internal lateinit var appPrefs: AppPrefs

    private val dataModel: DashboardFragmentDataModel by viewModels()

    // region Lifecycle Events

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        supportActivity.setSupportActionBar(binding.dashboardToolbar)
        setupSwipeRefreshLayout()
    }

    override fun onResume() {
        super.onResume()
        mEventBus.post(AnalyticsScreenEvent(SCREEN_DASHBOARD_CONNECT))
    }

    // endregion Lifecycle Events

    // region SwipeRefreshLayout
    private val refreshLayoutViewPagerHelper = SwipeRefreshLayoutViewPagerHelper()

    private fun setupSwipeRefreshLayout() {
        binding.refresh.setOnRefreshListener { dataModel.syncData(true) }
        refreshLayoutViewPagerHelper.swipeRefreshLayout = binding.refresh
        dataModel.syncTracker.isSyncing.observe(viewLifecycleOwner) {
            refreshLayoutViewPagerHelper.isRefreshing = it == true
        }
    }
    // endregion SwipeRefreshLayout

    override fun layoutRes() = R.layout.fragment_dashboard_new
}

@HiltViewModel
class DashboardFragmentDataModel @Inject constructor(
    appPrefs: AppPrefs,
    private val appealsSyncService: AppealsSyncService,
    private val donationsSyncService: DonationsSyncService,
    private val goalProgressSyncService: GoalProgressSyncService,
    private val tasksSyncService: TasksSyncService
) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val accountListId = accountListId.value ?: return

        syncTracker.runSyncTasks(
            appealsSyncService.syncAppeals(accountListId, force),
            goalProgressSyncService.syncGoalProgress(accountListId, force),
            tasksSyncService.syncTasks(accountListId, force),
            tasksSyncService.syncCompletedTasks(accountListId, 1, force)
        )
        for (i in 0..11) {
            var iYearMonth = YearMonth.now()
            if (i > 0) {
                iYearMonth = iYearMonth.plusMonths(- i.toLong())
            }
            syncTracker.runSyncTask(donationsSyncService.syncDonations(accountListId, iYearMonth, force))
        }
    }
    // endregion Sync logic
}
