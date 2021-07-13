package org.mpdx.android.features

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.fragment.app.commit
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter
import javax.inject.Inject
import org.mpdx.android.R
import org.mpdx.android.base.activity.BaseActivity
import org.mpdx.android.base.activity.DataBindingActivity
import org.mpdx.android.core.modal.ModalActivity.Companion.launchActivityForResult
import org.mpdx.android.databinding.ActivityDrawerMainBinding
import org.mpdx.android.features.analytics.model.ContactNewClickAnalyticsEvent
import org.mpdx.android.features.analytics.model.DonationNewClickAnalyticsEvent
import org.mpdx.android.features.analytics.model.TaskNewClickAnalyticsEvent
import org.mpdx.android.features.analytics.model.TaskNewLogClickAnalyticsEvent
import org.mpdx.android.features.appeals.createAppealsFragment
import org.mpdx.android.features.coaching.CoachingFragment
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivity
import org.mpdx.android.features.contacts.contacteditor.startContactEditorActivity
import org.mpdx.android.features.contacts.list.ContactsFragment
import org.mpdx.android.features.dashboard.createDashboardFragment
import org.mpdx.android.features.donations.add.AddDonationFragment
import org.mpdx.android.features.donations.list.DonationsFragment
import org.mpdx.android.features.notifications.list.NotificationsFragment
import org.mpdx.android.features.settings.SettingsFragment
import org.mpdx.android.features.sync.BatchSyncService
import org.mpdx.android.features.tasks.editor.buildTaskEditorActivityIntent
import org.mpdx.android.features.tasks.tasklist.TasksFragment

const val REQUEST_SETTINGS = 2

@AndroidEntryPoint
class MainActivity @Inject constructor() :
    BaseActivity(),
    DataBindingActivity<ActivityDrawerMainBinding>,
    Toolbar.OnMenuItemClickListener,
    NavigationView.OnNavigationItemSelectedListener,
    PageLoadListener {
    @Inject
    lateinit var appPrefs: AppPrefs
    @Inject
    lateinit var batchSyncService: BatchSyncService

    companion object {
        const val RESULT_CLOSE_ACTIVITY: Int = 5

        @JvmStatic
        fun getIntent(context: Context?, deepLinkType: String?, deepLinkId: String?): Intent? {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(EXTRA_DEEP_LINK_TYPE, deepLinkType)
            intent.putExtra(EXTRA_DEEP_LINK_ID, deepLinkId)
            return intent
        }
    }

    // region Data Binding
    override fun layoutId() = R.layout.activity_drawer_main
    override lateinit var binding: ActivityDrawerMainBinding

    override fun onCreateDataBinding(binding: ActivityDrawerMainBinding) {
        super.onCreateDataBinding(binding)
        setUpFabSpeedListener()
        setBottomBar()
        setNavigation()
    }
    // endregion Data Binding

    // region LifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return

        // sync any pending dirty data
        syncData()
        loadDefaultPageIfMissing()
        followDeepLinkIfAvailable()
    }

    // region LifeCycle

    private fun setNavigation() {
        binding.mainNavigationView.setNavigationItemSelectedListener(this)
    }

    private fun setUpFabSpeedListener() {
        binding.appBarMain.fabSpeedDial.setMenuListener(object : SimpleMenuListenerAdapter() {
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_new_task -> {
                        startActivity(buildTaskEditorActivityIntent())
                        mEventBus.post(TaskNewClickAnalyticsEvent)
                    }
                    R.id.action_log_task -> {
                        startActivity(buildTaskEditorActivityIntent(null, true))
                        mEventBus.post(TaskNewLogClickAnalyticsEvent)
                    }
                    R.id.action_new_contact -> {
                        startContactEditorActivity()
                        mEventBus.post(ContactNewClickAnalyticsEvent)
                    }
                    R.id.action_new_donation -> {
                        val addDonationFragment = AddDonationFragment.newInstance()
                        launchActivityForResult(
                            this@MainActivity,
                            addDonationFragment, DonationsFragment.REQUEST_ADD_DONATION
                        )
                        mEventBus.post(DonationNewClickAnalyticsEvent)
                    }
                }
                return true
            }
        })
    }

    private fun setBottomBar() {
        binding.appBarMain.bottomAppBar.setNavigationOnClickListener {
            binding.mainDrawerLayout.openDrawer(binding.mainNavigationView)
        }
        binding.appBarMain.bottomAppBar.setOnMenuItemClickListener(this)
    }

    private fun syncData() {
        batchSyncService.syncBaseData(appPrefs.accountListId).launch()
    }

    private fun followDeepLinkIfAvailable() {
        if (deepLinkType != null) {
            when (deepLinkType) {
                DEEP_LINK_TYPE_CONTACTS -> when (deepLinkId) {
                    null -> loadPage(Page.NOTIFICATIONS)
                    else -> {
                        loadPage(Page.CONTACTS)
                        startActivity(ContactDetailActivity.getIntent(this, deepLinkId))
                    }
                }
                DEEP_LINK_TYPE_TASK -> loadPage(Page.TASKS)
                DEEP_LINK_TYPE_COACHING -> loadPage(Page.COACHING)
            }
            deepLinkType = null
            deepLinkId = null
        }
    }

    private fun showFab() {
        (binding.appBarMain.fabSpeedDial.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            (behavior as? HideBottomViewOnScrollBehavior)?.apply {
                slideUp(binding.appBarMain.fabSpeedDial)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_CLOSE_ACTIVITY) finish()
    }

    // endregion Lifecycle

    // region Page Loading
    private fun loadDefaultPageIfMissing() {
        if (supportFragmentManager.primaryNavigationFragment == null) loadPage(Page.DASHBOARD)
    }

    override fun loadPage(page: Page) {
        val fragment = when (page) {
            Page.DASHBOARD -> createDashboardFragment(deepLinkId, deepLinkTime)
            Page.TASKS -> TasksFragment(deepLinkId, deepLinkTime)
            Page.CONTACTS -> ContactsFragment()
            Page.DONATIONS -> DonationsFragment()
            Page.NOTIFICATIONS -> NotificationsFragment.create(deepLinkId, deepLinkTime)
            Page.APPEALS -> createAppealsFragment(deepLinkId, deepLinkTime)
            Page.COACHING -> CoachingFragment.create(deepLinkId, deepLinkTime)
        }
        val tag = fragment.javaClass.simpleName
        supportFragmentManager.popBackStackImmediate(tag, POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment, tag)
            supportFragmentManager.primaryNavigationFragment?.let {
                addToBackStack(tag)
            }
            setPrimaryNavigationFragment(fragment)
        }
        showFab()
    }

    override fun onNavigationItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.nav_task -> {
            loadPage(Page.TASKS)
            true
        }
        R.id.nav_contacts -> {
            loadPage(Page.CONTACTS)
            true
        }
        R.id.nav_donations -> {
            loadPage(Page.DONATIONS)
            true
        }
        R.id.nav_dashboard -> {
            loadPage(Page.DASHBOARD)
            true
        }
        R.id.nav_notifications -> {
            loadPage(Page.NOTIFICATIONS)
            true
        }
        R.id.nav_appeals -> {
            loadPage(Page.APPEALS)
            true
        }
        R.id.nav_coaching -> {
            loadPage(Page.COACHING)
            true
        }
        else -> false
    }.also { binding.mainDrawerLayout.closeDrawer(binding.mainNavigationView) }
    // endregion Page Loading

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.settings -> {
                launchActivityForResult(this, SettingsFragment(), REQUEST_SETTINGS)
                true
            }
            R.id.menu_item_help -> {
                launchHelpDialog()
                true
            }
            else -> false
        }
    }
}
