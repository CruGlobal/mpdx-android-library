package org.mpdx.android.features.filter.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import com.h6ah4i.android.widget.advrecyclerview.composedadapter.ComposedAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.ccci.gto.android.common.recyclerview.util.lifecycleOwner
import org.mpdx.android.R
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.widget.recyclerview.SimpleLayoutAdapter
import org.mpdx.android.core.modal.ModalFragment
import org.mpdx.android.databinding.FiltersSelectorFragmentBinding
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_FILTER_CHURCH
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_FILTER_CITY
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_FILTER_LIKELY
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_FILTER_REFERRER
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_FILTER_STATE
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_FILTER_STATUS
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_FILTER_TAGS
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_FILTER_TIMEZONE
import org.mpdx.android.features.analytics.model.SCREEN_NOTIFICATION_FILTER
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_FILTER_ACTION_TYPE
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_FILTER_CONTACT_CHURCH
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_FILTER_CONTACT_CITY
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_FILTER_CONTACT_LIKELY_GIVE
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_FILTER_CONTACT_REFERRER
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_FILTER_CONTACT_STATE
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_FILTER_CONTACT_STATUS
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_FILTER_CONTACT_TIMEZONE
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_FILTER_TAGS
import org.mpdx.android.features.filter.model.Filter
import org.mpdx.android.features.filter.model.Filter.Companion.CONTAINER_CONTACT
import org.mpdx.android.features.filter.model.Filter.Companion.CONTAINER_NOTIFICATION
import org.mpdx.android.features.filter.model.Filter.Companion.CONTAINER_TASK
import org.mpdx.android.features.filter.realm.forContainer
import org.mpdx.android.features.filter.realm.getFilters
import org.mpdx.android.features.filter.realm.sortByName
import org.mpdx.android.features.filter.realm.type
import org.mpdx.android.features.filter.repository.FiltersRepository
import splitties.fragmentargs.arg

@AndroidEntryPoint
class FilterSelectorFragment() :
    DataBindingFragment<FiltersSelectorFragmentBinding>(),
    ModalFragment,
    FilterActions,
    BulkFilterActions {
    constructor(container: Int, type: Filter.Type) : this() {
        this.container = container
        this.type = type
    }

    private var container: Int by arg()
    private var type: Filter.Type by arg()

    @Inject
    internal lateinit var filtersRepository: FiltersRepository

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setupDataModel()
    }

    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FiltersSelectorFragmentBinding.inflate(inflater, container, false).also {
            it.type = type
            it.filterActions = this
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFiltersView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.multi_select_filter_menu, menu)
    }

    override fun onResume() {
        super.onResume()
        sendAnalyticsEvent()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.filter_menu_clear) {
            filtersRepository.toggleAllFilters(container, type, false)
        }
        return super.onOptionsItemSelected(item)
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel: FilterSelectorFragmentDataModel by viewModels()

    private fun setupDataModel() {
        dataModel.container.value = container
        dataModel.type.value = type
    }
    // endregion Data Model

    // region Filters UI
    private val filtersAdapter by lazy {
        FilterSelectorAdapter().also {
            it.filterActions.set(this)
            dataModel.filters.observe(this, it)
        }
    }

    private val adapter by lazy {
        ComposedAdapter().apply {
            addAdapter(SimpleLayoutAdapter(R.layout.filters_selector_header))
            addAdapter(filtersAdapter)
        }
    }

    private fun setupFiltersView() {
        binding.filters.also {
            it.lifecycleOwner = viewLifecycleOwner
            it.setHasFixedSize(true)
            it.adapter = adapter
        }
    }
    // endregion Filters UI

    // region Analytics
    private fun sendAnalyticsEvent() {
        when (container) {
            CONTAINER_CONTACT -> when (type) {
                Filter.Type.CONTACT_STATUS -> AnalyticsScreenEvent(SCREEN_CONTACTS_FILTER_STATUS)
                Filter.Type.CONTACT_CHURCH -> AnalyticsScreenEvent(SCREEN_CONTACTS_FILTER_CHURCH)
                Filter.Type.CONTACT_CITY -> AnalyticsScreenEvent(SCREEN_CONTACTS_FILTER_CITY)
                Filter.Type.CONTACT_LIKELY_TO_GIVE -> AnalyticsScreenEvent(SCREEN_CONTACTS_FILTER_LIKELY)
                Filter.Type.CONTACT_REFERRER -> AnalyticsScreenEvent(SCREEN_CONTACTS_FILTER_REFERRER)
                Filter.Type.CONTACT_STATE -> AnalyticsScreenEvent(SCREEN_CONTACTS_FILTER_STATE)
                Filter.Type.CONTACT_TAGS -> AnalyticsScreenEvent(SCREEN_CONTACTS_FILTER_TAGS)
                Filter.Type.CONTACT_TIMEZONE -> AnalyticsScreenEvent(SCREEN_CONTACTS_FILTER_TIMEZONE)
                else -> null
            }
            CONTAINER_TASK -> when (type) {
                Filter.Type.TASK_TAGS -> AnalyticsScreenEvent(SCREEN_TASKS_FILTER_TAGS)
                Filter.Type.CONTACT_CHURCH -> AnalyticsScreenEvent(SCREEN_TASKS_FILTER_CONTACT_CHURCH)
                Filter.Type.CONTACT_CITY -> AnalyticsScreenEvent(SCREEN_TASKS_FILTER_CONTACT_CITY)
                Filter.Type.CONTACT_LIKELY_TO_GIVE -> AnalyticsScreenEvent(SCREEN_TASKS_FILTER_CONTACT_LIKELY_GIVE)
                Filter.Type.CONTACT_REFERRER -> AnalyticsScreenEvent(SCREEN_TASKS_FILTER_CONTACT_REFERRER)
                Filter.Type.CONTACT_STATE -> AnalyticsScreenEvent(SCREEN_TASKS_FILTER_CONTACT_STATE)
                Filter.Type.CONTACT_TIMEZONE -> AnalyticsScreenEvent(SCREEN_TASKS_FILTER_CONTACT_TIMEZONE)
                Filter.Type.ACTION_TYPE -> AnalyticsScreenEvent(SCREEN_TASKS_FILTER_ACTION_TYPE)
                Filter.Type.CONTACT_STATUS -> AnalyticsScreenEvent(SCREEN_TASKS_FILTER_CONTACT_STATUS)
                else -> null
            }
            CONTAINER_NOTIFICATION -> when (type) {
                Filter.Type.NOTIFICATION_TYPES -> AnalyticsScreenEvent(SCREEN_NOTIFICATION_FILTER)
                else -> null
            }
            else -> null
        }?.let { eventBus.post(it) }
    }
    // endregion Analytics

    // region ModalFragment
    override fun getToolbar() = binding.toolbar
    // endregion ModalFragment

    // region FilterActions
    override fun toggleFilter(filter: Filter?, isEnabled: Boolean) =
        filtersRepository.toggleFilter(filter?.container, filter?.type, filter?.key, isEnabled)
    // endregion FilterActions

    // region BulkFilterActions
    override fun toggleAllFilters(isEnabled: Boolean) = filtersRepository.toggleAllFilters(container, type, isEnabled)
    override fun toggleContactStatusFilters(selectActive: Boolean) =
        filtersRepository.toggleContactStatusFilters(container, showActive = selectActive)
    // endregion BulkFilterActions
}

internal class FilterSelectorFragmentDataModel : RealmViewModel() {
    val container = MutableLiveData<Int>()
    val type = MutableLiveData<Filter.Type>()

    val filters by lazy {
        container.switchCombineWith(type) { container, type ->
            realm.getFilters().forContainer(container).type(type).sortByName().asLiveData()
        }
    }
}
