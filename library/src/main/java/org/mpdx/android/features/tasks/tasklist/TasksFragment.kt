package org.mpdx.android.features.tasks.tasklist

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.databinding.TasksListFragmentBinding
import org.mpdx.android.features.base.fragments.BindingFragment
import org.mpdx.android.features.filter.buildFiltersIntent
import org.mpdx.android.features.filter.model.Filter
import org.mpdx.android.features.filter.realm.getTaskFilters
import org.mpdx.android.features.filter.realm.isEnabled
import org.mpdx.android.utils.updateBadgeNumber

@AndroidEntryPoint
class TasksFragment(deepLinkId: String? = null, deepLinkTime: Long = 0) : BindingFragment<TasksListFragmentBinding>() {
    init {
        setDeepLinkId(deepLinkId, deepLinkTime)
    }

    private val dataModel: TasksFragmentDataModel by viewModels()

    // region Lifecycle
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupPages()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.tasks_menu, menu)
        menu.findItem(R.id.action_filter)?.setupFilterAction() ?: cleanupFilterAction()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        updateFilterAction()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                startActivity(requireContext().buildFiltersIntent(Filter.CONTAINER_TASK))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyOptionsMenu() {
        cleanupFilterAction()
        super.onDestroyOptionsMenu()
    }
    // endregion Lifecycle

    // region Filter Action
    private var filterMenuItem: MenuItem? = null
    private val filterMenuItemCountObserver = Observer<Int> { updateFilterAction(it) }

    private fun MenuItem.setupFilterAction() {
        filterMenuItem = this
        dataModel.filterCount.observe(this@TasksFragment, filterMenuItemCountObserver)
    }

    private fun updateFilterAction(count: Int = dataModel.filterCount.value ?: 0) {
        filterMenuItem
            ?.updateBadgeNumber(ContextThemeWrapper(requireContext(), R.style.Theme_Mpdx_Filters_Action), count)
    }

    private fun cleanupFilterAction() {
        dataModel.filterCount.removeObserver(filterMenuItemCountObserver)
        filterMenuItem = null
    }
    // endregion Filter Action

    override fun layoutRes() = R.layout.tasks_list_fragment

    private fun setupToolbar() {
        supportActivity.setSupportActionBar(binding.toolbar)
        binding.toolbar.title = getString(R.string.tasks).toLowerCase(Locale.getDefault())
    }

    private fun setupPages() {
        binding.pager.adapter = TasksPagerAdapter(requireContext(), childFragmentManager, deepLinkId, deepLinkTime)
        binding.tabs.setupWithViewPager(binding.pager)
    }
}

class TasksFragmentDataModel : RealmViewModel() {
    private val filters = realm.getTaskFilters().isEnabled().asLiveData()

    internal val filterCount by lazy { filters.map { it.size }.distinctUntilChanged() }
}
