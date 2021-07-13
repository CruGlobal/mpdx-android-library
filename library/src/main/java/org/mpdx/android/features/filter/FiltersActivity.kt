package org.mpdx.android.features.filter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.databinding.ObservableField
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.h6ah4i.android.widget.advrecyclerview.composedadapter.ComposedAdapter
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import io.realm.Sort
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.recyclerview.adapter.SimpleDataBindingAdapter
import org.mpdx.android.R
import org.mpdx.android.base.activity.BaseActivity
import org.mpdx.android.base.activity.DataBindingActivity
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.widget.recyclerview.SimpleLayoutAdapter
import org.mpdx.android.base.widget.recyclerview.UniqueItemDataBindingAdapter
import org.mpdx.android.core.modal.ModalActivity
import org.mpdx.android.databinding.FiltersActivityBinding
import org.mpdx.android.databinding.FiltersSectionEnabledBinding
import org.mpdx.android.databinding.FiltersSectionTypeBinding
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_FILTER
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_FILTER
import org.mpdx.android.features.filter.model.Filter
import org.mpdx.android.features.filter.model.FilterFields
import org.mpdx.android.features.filter.realm.forContainer
import org.mpdx.android.features.filter.realm.getFilters
import org.mpdx.android.features.filter.realm.isEnabled
import org.mpdx.android.features.filter.repository.FiltersRepository
import org.mpdx.android.features.filter.selector.FilterActions
import org.mpdx.android.features.filter.selector.FilterSelectorFragment

private const val ARG_CONTAINER = "container"

fun Context.buildFiltersIntent(container: Int): Intent = Intent(this, FiltersActivity::class.java)
    .putExtra(ARG_CONTAINER, container)

@AndroidEntryPoint
class FiltersActivity :
    BaseActivity(), DataBindingActivity<FiltersActivityBinding>, FilterActions, OnTypeClickedListener {
    private val container: Int
        get() = intent?.getIntExtra(ARG_CONTAINER, Filter.CONTAINER_CONTACT) ?: Filter.CONTAINER_CONTACT

    @Inject
    internal lateinit var filtersRepository: FiltersRepository

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDataModel()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        setupToolbar()
        setupFilters()
    }

    override fun onResume() {
        super.onResume()
        sendAnalyticsEvent()
    }

    override fun onTypeClicked(type: Filter.Type?) {
        if (type != null) ModalActivity.launchActivity(this, FilterSelectorFragment(container, type))
    }

    override fun onSupportNavigateUp(): Boolean {
        // suppress native up navigation for a simpler finish() implementation
        finish()
        return true
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel: FiltersActivityDataModel by viewModels()

    private fun setupDataModel() {
        dataModel.container.value = container
    }
    // endregion Data Model

    // region Data Binding
    override lateinit var binding: FiltersActivityBinding
    override fun layoutId() = R.layout.filters_activity
    // endregion Data Binding

    // region RecyclerView
    private val enabledAdapter by lazy {
        EnabledFiltersAdapter(this, dataModel.enabledFilters).also { it.filterActions.set(this) }
    }
    private val typesAdapter by lazy {
        FilterTypesAdapter(this).also {
            it.typeClickedListener.set(this)
            dataModel.filterTypes.observe(this, it)
        }
    }

    private fun setupFilters() {
        binding.filters.adapter = ComposedAdapter().apply {
            addAdapter(enabledAdapter)
            addAdapter(SimpleLayoutAdapter(R.layout.filters_section_types_header))
            addAdapter(typesAdapter)
        }
    }
    // endregion RecyclerView

    // region FilterActions
    override fun toggleFilter(filter: Filter?, isEnabled: Boolean) =
        filtersRepository.toggleFilter(filter?.container, filter?.type, filter?.key, isEnabled)
    // endregion FilterActions

    override fun setupToolbar() {
        setTitle(
            when (container) {
                Filter.CONTAINER_CONTACT -> R.string.toolbar_filter_contacts
                Filter.CONTAINER_TASK -> R.string.toolbar_filter_tasks
                else -> R.string.toolbar_filter_contacts
            }
        )
        setSupportActionBar(binding.toolbar)
        super.setupToolbar()
    }

    private fun sendAnalyticsEvent() {
        mEventBus.post(
            AnalyticsScreenEvent(
                when (container) {
                    Filter.CONTAINER_TASK -> SCREEN_TASKS_FILTER
                    Filter.CONTAINER_CONTACT -> SCREEN_CONTACTS_FILTER
                    else -> SCREEN_CONTACTS_FILTER
                }
            )
        )
    }
}

@HiltViewModel
class FiltersActivityDataModel @Inject constructor(
    appPrefs: AppPrefs,
    private val context: Context,
    private val filtersRepository: FiltersRepository
) : RealmViewModel() {
    private val accountListId = appPrefs.accountListIdLiveData
    internal val container = MutableLiveData<Int>()

    private val distinctContainer = container.distinctUntilChanged()
    val filters by lazy { distinctContainer.switchMap { realm.getFilters().forContainer(it).asLiveData() } }
    val filterTypes by lazy { filters.map { it.mapNotNull(Filter::type).distinct().sortedBy(Filter.Type::ordinal) } }

    val enabledFilters by lazy {
        distinctContainer.switchMap {
            realm.getFilters().forContainer(it).isEnabled()
                .sort(FilterFields._TYPE, Sort.ASCENDING, FilterFields.LABEL, Sort.ASCENDING).asLiveData()
        }
    }

    // region Data Sync
    init {
        accountListId.observe(this) { updateFilters() }
        distinctContainer.observe(this) { updateFilters() }
    }

    private fun updateFilters() {
        val accountListId = accountListId.value ?: return

        when (container.value) {
            Filter.CONTAINER_CONTACT -> filtersRepository.updateContactFilters(context)
            Filter.CONTAINER_TASK -> filtersRepository.updateTaskFilters(accountListId, context)
            Filter.CONTAINER_NOTIFICATION -> filtersRepository.updateNotificationFilters()
        }
    }
    // endregion Data Sync
}

private class EnabledFiltersAdapter(
    lifecycleOwner: LifecycleOwner,
    private val enabledFilters: LiveData<RealmResults<Filter>>
) : SimpleDataBindingAdapter<FiltersSectionEnabledBinding>(lifecycleOwner) {
    val filterActions = ObservableField<FilterActions>()

    override fun getItemCount() = 1
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        FiltersSectionEnabledBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            it.filterActions = filterActions
            it.enabledFilters = enabledFilters.map { it.map { it to parent.context.getFilterLabel(it) }.toMap() }
        }

    override fun onBindViewDataBinding(binding: FiltersSectionEnabledBinding, position: Int) = Unit

    private inline fun Context.getFilterLabel(filter: Filter) =
        getString(R.string.filters_filter_label, filter.type?.label?.let { getString(it) }, filter.label)
}

private class FilterTypesAdapter(lifecycleOwner: LifecycleOwner) :
    UniqueItemDataBindingAdapter<Filter.Type, FiltersSectionTypeBinding>(lifecycleOwner) {
    val typeClickedListener = ObservableField<OnTypeClickedListener>()

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        FiltersSectionTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            it.typeClickListener = typeClickedListener
        }

    override fun onBindViewDataBinding(binding: FiltersSectionTypeBinding, position: Int) {
        binding.type = getItem(position)
    }
}

interface OnTypeClickedListener {
    fun onTypeClicked(type: Filter.Type?)
}
