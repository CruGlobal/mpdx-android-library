package org.mpdx.android.features.contacts.list

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Sort
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.ccci.gto.android.common.recyclerview.util.lifecycleOwner
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.filterByQuery
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.databinding.ContactsListFragmentBinding
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.FilterAppliedAnalyticsEvent
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS
import org.mpdx.android.features.base.fragments.BindingFragment
import org.mpdx.android.features.contacts.ContactClickListener
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivity
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.ContactFields
import org.mpdx.android.features.contacts.realm.applyFilters
import org.mpdx.android.features.contacts.realm.getContacts
import org.mpdx.android.features.contacts.realm.hasName
import org.mpdx.android.features.contacts.realm.hasVisibleStatus
import org.mpdx.android.features.contacts.repository.ContactsRepository
import org.mpdx.android.features.contacts.sync.ContactsSyncService
import org.mpdx.android.features.filter.buildFiltersIntent
import org.mpdx.android.features.filter.model.Filter
import org.mpdx.android.features.filter.realm.getContactFilters
import org.mpdx.android.features.filter.realm.isEnabled
import org.mpdx.android.utils.hideKeyboard
import org.mpdx.android.utils.updateBadgeNumber

@AndroidEntryPoint
class ContactsFragment : BindingFragment<ContactsListFragmentBinding>(), ContactClickListener {
    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        supportActivity.setSupportActionBar(binding.toolbar)
        setupSwipeRefreshLayout()
        setupContactsList()
        dataModel.contacts.observe(viewLifecycleOwner) { showContactCount(it.size) }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.contacts_menu, menu)
        menu.findItem(R.id.search)?.setupSearchAction()
        menu.findItem(R.id.action_filter)?.setupFilterAction() ?: cleanupFilterAction()
    }

    override fun onResume() {
        super.onResume()
        mEventBus.post(AnalyticsScreenEvent(SCREEN_CONTACTS))
        sendFilteredAnalyticsEvent()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                startActivity(requireContext().buildFiltersIntent(Filter.CONTAINER_CONTACT))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyOptionsMenu() {
        cleanupFilterAction()
        super.onDestroyOptionsMenu()
    }

    override fun onDestroyView() {
        cleanupSwipeRefreshLayout()
        super.onDestroyView()
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel by lazy {
        ViewModelProvider(this).get(ContactsFragmentDataModel::class.java)
            .also { it.filterCount.observe(this, this::sendFilteredAnalyticsEvent) }
    }
    // endregion Data Model

    // region Analytics
    private fun sendFilteredAnalyticsEvent(count: Int = dataModel.filterCount.value ?: 0) {
        if (count > 0) mEventBus.post(FilterAppliedAnalyticsEvent)
    }
    // endregion Analytics

    override fun layoutRes() = R.layout.contacts_list_fragment

    private fun showContactCount(count: Int) {
        binding.contactsCount.text = getString(R.string.contact_count_text, count)
    }

    // region Search Action
    private fun MenuItem.setupSearchAction() {
        (actionView as? SearchView)?.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    dataModel.query.value = query
                    view?.hideKeyboard()
                    return true
                }

                override fun onQueryTextChange(query: String): Boolean {
                    dataModel.query.value = query
                    return true
                }
            })
            queryHint = getString(R.string.search_hint)
        }
    }
    // endregion Search Action

    // region Filter Action
    private var filterMenuItem: MenuItem? = null
    private val filterMenuItemCountObserver = Observer<Int> {
        filterMenuItem?.updateBadgeNumber(ContextThemeWrapper(requireContext(), R.style.Theme_Mpdx_Filters_Action), it)
    }

    private fun MenuItem.setupFilterAction() {
        filterMenuItem = this
        dataModel.filterCount.observe(this@ContactsFragment, filterMenuItemCountObserver)
    }

    private fun cleanupFilterAction() {
        dataModel.filterCount.removeObserver(filterMenuItemCountObserver)
        filterMenuItem = null
    }
    // endregion Filter Action

    // region SwipeRefreshLayout
    private fun setupSwipeRefreshLayout() {
        binding.refresh.apply {
            dataModel.syncTracker.isSyncing.observe(viewLifecycleOwner) { isRefreshing = it }
            setOnRefreshListener { dataModel.syncData(true) }
        }
    }

    private fun cleanupSwipeRefreshLayout() {
        binding.refresh.setOnRefreshListener(null)
    }
    // endregion SwipeRefreshLayout

    // region Contacts List
    @Inject
    internal lateinit var contactsRepository: ContactsRepository
    private val adapter by lazy {
        ContactsAdapter(contactsRepository).also {
            it.contactClickListener.set(this)
            dataModel.contacts.observe(this, it)
        }
    }

    private fun setupContactsList() {
        binding.contacts.let {
            it.lifecycleOwner = viewLifecycleOwner
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(it.context, DividerItemDecoration.VERTICAL))
        }
    }
    // endregion Contacts List

    // region ContactClickListener
    override fun onContactClick(contact: Contact?) {
        contact?.id?.let { startActivity(ContactDetailActivity.getIntent(requireActivity(), it)) }
    }
    // endregion ContactClickListener
}

@HiltViewModel
class ContactsFragmentDataModel @Inject constructor(
    appPrefs: AppPrefs,
    private val contactsSyncService: ContactsSyncService
) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData
    val query = MutableLiveData("")

    private val filters = realm.getContactFilters().isEnabled().asLiveData()
    val contacts =
        accountListId.switchCombineWith(query.distinctUntilChanged(), filters) { accountListId, query, filters ->
            realm.getContacts(accountListId)
                .hasName()
                .filterByQuery(ContactFields.NAME, query)
                .apply { if (filters.none { it.type == Filter.Type.CONTACT_STATUS }) hasVisibleStatus() }
                .applyFilters(filters)
                .sort(ContactFields.IS_STARRED, Sort.DESCENDING, ContactFields.NAME, Sort.ASCENDING)
                .asLiveData()
        }

    val filterCount by lazy { filters.map { it.size }.distinctUntilChanged() }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val accountListId = accountListId.value ?: return
        syncTracker.runSyncTasks(
            contactsSyncService.syncContacts(accountListId, force),
            contactsSyncService.syncDeletedContacts(force)
        )
    }
    // endregion Sync logic
}
