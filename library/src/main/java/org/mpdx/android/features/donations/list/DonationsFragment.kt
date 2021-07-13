package org.mpdx.android.features.donations.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.viewpager.widget.ViewPager
import dagger.hilt.android.AndroidEntryPoint
import org.ccci.gto.android.common.viewpager.widget.SwipeRefreshLayoutViewPagerHelper
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.databinding.FragmentDonationsBinding
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_DONATIONS_MONTH
import org.mpdx.android.utils.get
import org.mpdx.android.utils.indexOf
import org.threeten.bp.YearMonth

private const val MONTHS = 600L

@AndroidEntryPoint
class DonationsFragment : DataBindingFragment<FragmentDonationsBinding>() {
    companion object {
        // TODO: Request constants should be defined where ever they are used to launch this fragment
        const val REQUEST_ADD_DONATION = 7
        const val REQUEST_EDIT_DONATION = 8
        // TODO: the result constant should be defined where ever it is being set as a result
        const val RESULT_DONATION_ADDED = 3
    }

    // region Lifecycle
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDonationsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appCompatActivity?.setSupportActionBar(binding.donationsToolbar)
        setupSwipeRefreshLayout()
        setupViewPager()
    }

    override fun onResume() {
        super.onResume()
        eventBus.post(AnalyticsScreenEvent(SCREEN_DONATIONS_MONTH))
    }

    override fun onDestroyView() {
        cleanupSwipeRefreshLayout()
        super.onDestroyView()
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel: DonationsFragmentViewModel by viewModels()
    // endregion Data Model

    // region SwipeRefreshLayout
    private val refreshLayoutViewPagerHelper = SwipeRefreshLayoutViewPagerHelper()

    private fun setupSwipeRefreshLayout() {
        binding.refresh.setOnRefreshListener { dataModel.syncData(true) }
        refreshLayoutViewPagerHelper.swipeRefreshLayout = binding.refresh
        dataModel.syncTracker.isSyncing.observe(viewLifecycleOwner) {
            refreshLayoutViewPagerHelper.isRefreshing = it == true
        }
    }

    private fun cleanupSwipeRefreshLayout() {
        refreshLayoutViewPagerHelper.swipeRefreshLayout = null
        binding.refresh.setOnRefreshListener(null)
    }
    // endregion SwipeRefreshLayout

    // region ViewPager
    private val months = YearMonth.now().let { it.minusMonths(MONTHS)..it }
    private val pagerAdapter by lazy {
        DonationsPagerAdapter(this, dataModel)
            .also { it.months = months }
    }
    private val currentMonthPageListener = object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            dataModel.currentMonth.value = months[position]
        }
    }

    private fun setupViewPager() {
        binding.viewPagerDonations.apply {
            adapter = pagerAdapter
            addOnPageChangeListener(currentMonthPageListener)
            addOnPageChangeListener(refreshLayoutViewPagerHelper)
            currentItem = months.indexOf(dataModel.currentMonth.value ?: YearMonth.now())
            binding.tabLayoutDonations.setUpWithViewPager(this)
        }
    }
    // endregion ViewPager
}
