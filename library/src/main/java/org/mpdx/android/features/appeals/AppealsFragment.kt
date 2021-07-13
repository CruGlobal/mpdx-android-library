package org.mpdx.android.features.appeals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.viewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Sort
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.core.sync.AccountListSyncService
import org.mpdx.android.databinding.FragmentAppealsBinding
import org.mpdx.android.databinding.ViewAppealBinding
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_APPEALS_LIST
import org.mpdx.android.features.appeals.details.buildAppealDetailsActivityIntent
import org.mpdx.android.features.appeals.model.Appeal
import org.mpdx.android.features.appeals.model.AppealFields
import org.mpdx.android.features.appeals.realm.getAppeal
import org.mpdx.android.features.appeals.realm.getAppeals
import org.mpdx.android.features.appeals.realm.hasName
import org.mpdx.android.features.appeals.sync.AppealsSyncService
import org.mpdx.android.features.base.fragments.BindingFragment

fun createAppealsFragment(id: String?, deepLinkTime: Long) = AppealsFragment()
    .apply { setDeepLinkId(id, deepLinkTime) }

@AndroidEntryPoint
class AppealsFragment : BindingFragment<FragmentAppealsBinding>(), AppealSelectedListener {
    private val viewModel: AppealsFragmentViewModel by viewModels()

    @Inject
    internal lateinit var appPrefs: AppPrefs

    // region Lifecycle Events
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        supportActivity.setSupportActionBar(binding.appealsToolbar)

        setupDataBinding()
        setupSwipeRefreshLayout()
        setupOtherAppeals()
    }

    override fun onResume() {
        super.onResume()
        mEventBus.post(AnalyticsScreenEvent(SCREEN_APPEALS_LIST))
    }

    override fun onAppealSelected(appeal: Appeal?) {
        appeal?.id?.let { startActivity(requireContext().buildAppealDetailsActivityIntent(it)) }
    }

    override fun onDestroyView() {
        cleanupOtherAppeals()
        cleanupSwipeRefreshLayout()
        cleanupDataBinding()
        super.onDestroyView()
    }
    // endregion Lifecycle Events

    override fun layoutRes() = R.layout.fragment_appeals

    // region Data Binding
    private fun setupDataBinding() {
        binding.appealListener = ObservableField<AppealSelectedListener>(this)
        binding.primaryAppeal = viewModel.primaryAppeal
        binding.otherAppeals = viewModel.otherAppeals
    }

    private fun cleanupDataBinding() {
        binding.appealListener?.set(null)
    }
    // endregion Data Binding

    // region SwipeToRefreshLayout
    private fun setupSwipeRefreshLayout() {
        binding.refreshLayout.setOnRefreshListener { viewModel.syncData(true) }
        viewModel.syncTracker.isSyncing.observe(viewLifecycleOwner) {
            binding.refreshLayout.isRefreshing = it == true
        }
    }

    private fun cleanupSwipeRefreshLayout() {
        binding.refreshLayout.setOnRefreshListener(null)
    }
    // endregion SwipeToRefreshLayout

    // region Other Appeals

    private val otherAppealsAdapter by lazy {
        AppealsAdapter()
            .also { it.listener.set(this) }
            .also { viewModel.otherAppeals.observe(this, it) }
    }

    private fun setupOtherAppeals() {
        binding.otherAppealsView.apply {
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = otherAppealsAdapter
        }
    }

    private fun cleanupOtherAppeals() {
        binding.otherAppealsView.adapter = null
    }

    // endregion Other Appeals
}

@HiltViewModel
class AppealsFragmentViewModel @Inject constructor(
    appPrefs: AppPrefs,
    private val accountListSyncService: AccountListSyncService,
    private val appealsSyncService: AppealsSyncService
) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData
    val accountList by lazy { accountListId.switchMap { id: String? -> realm.getAccountList(id).firstAsLiveData() } }

    private val primaryAppealId by lazy { accountList.map { it?.primaryAppealId }.distinctUntilChanged() }
    val primaryAppeal by lazy { primaryAppealId.switchMap { realm.getAppeal(it).firstAsLiveData() } }

    val otherAppeals by lazy {
        accountListId.switchCombineWith(primaryAppealId) { accountList, primaryAppeal ->
            realm.getAppeals(accountList).hasName().and().notEqualTo(AppealFields.ID, primaryAppeal)
                .sort(AppealFields.CREATED_AT, Sort.DESCENDING).asLiveData()
        }
    }

    // region Sync Logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData(false) }
    }

    fun syncData(force: Boolean = false) {
        // XXX: this could be simplified to syncing a single account list if necessary
        syncTracker.runSyncTask(accountListSyncService.syncAccountLists(force))

        val accountListId = accountListId.value ?: return
        syncTracker.runSyncTask(appealsSyncService.syncAppeals(accountListId, force))
    }
    // endregion Sync Logic
}

internal class AppealsAdapter : UniqueItemRealmDataBindingAdapter<Appeal, ViewAppealBinding>() {
    val listener = ObservableField<AppealSelectedListener>()

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ViewAppealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            .also { it.listener = listener }

    override fun onBindViewDataBinding(binding: ViewAppealBinding, position: Int) {
        binding.appeal = getItem(position)
    }
}
