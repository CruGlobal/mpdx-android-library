package org.mpdx.android.features.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.databinding.ObservableField
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.switchMap
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Sort
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.viewpager.widget.SwipeRefreshLayoutViewPagerHelper
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.core.modal.ModalFragment
import org.mpdx.android.databinding.FragmentNotificationSettingsBinding
import org.mpdx.android.databinding.NotificationSettingRowBinding
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_SETTINGS_NOTIFICATION_PREFERENCES
import org.mpdx.android.features.base.fragments.BindingFragment
import org.mpdx.android.features.coaching.realm.getCoachingAccountLists
import org.mpdx.android.features.constants.ConstantsSyncService
import org.mpdx.android.features.constants.model.CruHashes
import org.mpdx.android.features.constants.realm.getConstants
import org.mpdx.android.features.notifications.model.NotificationType
import org.mpdx.android.features.settings.model.NotificationPreference
import org.mpdx.android.features.settings.model.NotificationPreferenceFields
import org.mpdx.android.features.settings.realm.getNotificationPreference
import org.mpdx.android.features.settings.realm.getNotificationPreferences
import org.mpdx.android.features.settings.sync.NotificationPreferencesSyncService
import org.mpdx.android.utils.realmTransactionAsync

private const val PREF_TASK_DUE = "task_due"
private const val PREF_COACHING_REMINDER = "coaching_reminder"

@AndroidEntryPoint
class NotificationSettingsFragment :
    BindingFragment<FragmentNotificationSettingsBinding>(),
    ModalFragment,
    NotificationPreferenceToggleListener {
    @Inject
    internal lateinit var appPrefs: AppPrefs
    @Inject
    internal lateinit var notificationPreferencesSyncService: NotificationPreferencesSyncService

    private val viewModel: NotificationSettingsFragmentViewModel by viewModels()

    // region Lifecycle
    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefreshLayout()
    }

    override fun onResume() {
        super.onResume()
        mEventBus.post(AnalyticsScreenEvent(SCREEN_SETTINGS_NOTIFICATION_PREFERENCES))
    }

    override fun onTogglePreference(preference: NotificationPreference, isChecked: Boolean) =
        togglePreference(preference, isChecked)

    override fun onDestroyView() {
        cleanupRecyclerView()
        cleanupSwipeRefreshLayout()
        super.onDestroyView()
    }
    // endregion Lifecycle

    // region Preferences
    private fun customPreference(notificationId: String, @StringRes label: Int, enabled: Boolean) =
        NotificationPreference().apply {
            id = notificationId
            notificationType = NotificationType().apply {
                description = getString(label)
            }
            isEnabledForApp = enabled
        }

    @MainThread
    private fun togglePreference(preference: NotificationPreference, isChecked: Boolean) {
        if (preference.isEnabledForApp == isChecked) return

        when (val id = preference.id) {
            PREF_TASK_DUE -> appPrefs.isTaskDueNotificationEnabled = isChecked
            PREF_COACHING_REMINDER -> appPrefs.hasCoachWeeklyNotification = isChecked
            else -> {
                realmTransactionAsync(
                    onSuccess = notificationPreferencesSyncService.syncDirtyNotificationPreferences()::launch
                ) {
                    getNotificationPreference(id).findFirst()?.apply {
                        trackingChanges = true
                        isEnabledForApp = isChecked
                        trackingChanges = false
                    }
                }
            }
        }
    }
    // endregion Preferences

    // region RecyclerView

    private val preferencesAdapter: NotificationPreferencesAdapter by lazy {
        NotificationPreferencesAdapter().also {
            it.toggleListener.set(this)
            viewModel.preferences.observe(this, it)
            viewModel.constantList.observe(this) { constants ->
                it.notificationTypes = constants?.notificationTypes
            }
            viewModel.hasCoachees.observe(this) { _ -> it.updateFixedPreferences() }
            appPrefs.hasCoachWeeklyNotificationLiveData.observe(this) { _ -> it.updateFixedPreferences() }
            appPrefs.isTaskDueNotificationEnabledLiveData.observe(this) { _ -> it.updateFixedPreferences() }
            it.updateFixedPreferences()
        }
    }

    private fun setupRecyclerView() {
        binding.preferences.adapter = preferencesAdapter
    }

    private fun NotificationPreferencesAdapter.updateFixedPreferences() {
        val preferences = mutableListOf(
            customPreference(
                PREF_TASK_DUE,
                R.string.settings_notifications_task_due,
                appPrefs.isTaskDueNotificationEnabled
            )
        )
        if (viewModel.hasCoachees.value == true) preferences.add(
            customPreference(
                PREF_COACHING_REMINDER,
                R.string.settings_notifications_coaching_weekly,
                appPrefs.hasCoachWeeklyNotification
            )
        )

        fixedPreferences = preferences
    }

    private fun cleanupRecyclerView() {
        binding.preferences.adapter = null
    }

    // endregion RecyclerView

    // region SwipeRefreshLayout
    private val refreshLayoutViewPagerHelper = SwipeRefreshLayoutViewPagerHelper()
    private val refreshLayoutObserver = Observer<Boolean?> { refreshLayoutViewPagerHelper.isRefreshing = it == true }

    private fun setupSwipeRefreshLayout() {
        binding.refreshNotifications.setOnRefreshListener { viewModel.syncData(true) }
        refreshLayoutViewPagerHelper.swipeRefreshLayout = binding.refreshNotifications
        viewModel.syncTracker.isSyncing.observe(viewLifecycleOwner, refreshLayoutObserver)
    }

    private fun cleanupSwipeRefreshLayout() {
        refreshLayoutViewPagerHelper.swipeRefreshLayout = null
        binding.refreshNotifications.setOnRefreshListener(null)
        viewModel.syncTracker.isSyncing.removeObserver(refreshLayoutObserver)
    }
    // endregion SwipeRefreshLayout
    override fun layoutRes() = R.layout.fragment_notification_settings
    override fun getToolbar() = binding.notificationSettingsToolbar
}

@HiltViewModel
class NotificationSettingsFragmentViewModel @Inject constructor(
    appPrefs: AppPrefs,
    private val notificationPreferencesSyncService: NotificationPreferencesSyncService,
    private val constantsSyncService: ConstantsSyncService
) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData

    val preferences by lazy {
        accountListId.switchMap {
            realm.getNotificationPreferences(it)
                .sort(NotificationPreferenceFields.CREATED_AT, Sort.ASCENDING)
                .asLiveData()
        }
    }

    val constantList = realm.getConstants().firstAsLiveData()

    val hasCoachees = Transformations.map(realm.getCoachingAccountLists().asLiveData()) { (it?.size ?: 0) > 0 }

    // region Sync Logic

    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val tasks = mutableListOf(constantsSyncService.sync(force = force))
        accountListId.value?.let { tasks += notificationPreferencesSyncService.syncNotificationPreferences(it, force) }
        syncTracker.runSyncTasks(*tasks.toTypedArray())
    }

    // endregion Sync Logic
}

class NotificationPreferencesAdapter :
    UniqueItemRealmDataBindingAdapter<NotificationPreference, NotificationSettingRowBinding>() {
    val toggleListener = ObservableField<NotificationPreferenceToggleListener>()
    var fixedPreferences: List<NotificationPreference>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var notificationTypes: List<CruHashes>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun dataOffset() = fixedPreferences?.size ?: 0
    override fun getItemCount() = super.getItemCount() + dataOffset()
    override fun getItem(index: Int) = when {
        index < dataOffset() -> fixedPreferences?.get(index)
        else -> super.getItem(index - dataOffset())
    }

    // region Lifecycle

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        NotificationSettingRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            .also { it.toggleListener = toggleListener }

    override fun onBindViewDataBinding(binding: NotificationSettingRowBinding, position: Int) {
        val preference = getItem(position)
        binding.preference = preference
        binding.notificationType = notificationTypes?.firstOrNull { it.key == preference?.notificationType?.id }
    }

    // endregion Lifecycle
}

interface NotificationPreferenceToggleListener {
    fun onTogglePreference(preference: NotificationPreference, isChecked: Boolean)
}
