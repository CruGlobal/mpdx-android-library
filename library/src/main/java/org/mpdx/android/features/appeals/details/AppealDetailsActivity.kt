package org.mpdx.android.features.appeals.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.orEmpty
import org.ccci.gto.android.common.viewpager.util.setScroller
import org.ccci.gto.android.common.viewpager.widget.SwipeRefreshLayoutViewPagerHelper
import org.mpdx.android.R
import org.mpdx.android.base.activity.BaseActivity
import org.mpdx.android.base.activity.DataBindingActivity
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.core.modal.ModalActivity
import org.mpdx.android.databinding.ActivityAppealsDetailBinding
import org.mpdx.android.features.appeals.AddCommitmentFragment
import org.mpdx.android.features.appeals.model.AskedContact
import org.mpdx.android.features.appeals.model.Pledge
import org.mpdx.android.features.appeals.realm.forAppeal
import org.mpdx.android.features.appeals.realm.getAppeal
import org.mpdx.android.features.appeals.realm.getAskedContacts
import org.mpdx.android.features.appeals.realm.getPledges
import org.mpdx.android.features.appeals.realm.hasName
import org.mpdx.android.features.appeals.realm.sortByName
import org.mpdx.android.features.appeals.realm.status
import org.mpdx.android.features.appeals.repository.AppealsRepository
import org.mpdx.android.features.appeals.sync.AppealsSyncService
import org.mpdx.android.features.appeals.sync.AskedContactsSyncService
import org.mpdx.android.features.appeals.sync.ExcludedAppealContactsSyncService
import org.mpdx.android.features.appeals.sync.PledgesSyncService
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.donations.realm.forAppeal
import org.mpdx.android.features.donations.realm.getDonations
import org.mpdx.android.features.donations.realm.sortByDate
import org.mpdx.android.features.selector.OnItemSelectedListener
import splitties.fragmentargs.arg

private const val ARG_APPEAL = "appealId"

fun Context.buildAppealDetailsActivityIntent(appealId: String) = Intent(this, AppealDetailsActivity::class.java)
    .putExtra(ARG_APPEAL, appealId)

@AndroidEntryPoint
class AppealDetailsActivity :
    BaseActivity(),
    DataBindingActivity<ActivityAppealsDetailBinding>,
    AddCommitmentListener,
    OnItemSelectedListener<Contact> {
    private val appealId: String? get() = intent?.getStringExtra(ARG_APPEAL)

    @Inject
    internal lateinit var appealsRepository: AppealsRepository

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (appealId == null) finish()
        if (isFinishing) return

        initDataModel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.appeals_details_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onContentChanged() {
        super.onContentChanged()
        setupToolbar()
        setupSwipeRefreshLayout()
        setupViewPager()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_add -> {
            showAddDialog()
            true
        }
        R.id.action_help -> {
            launchHelpDialog()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onAddCommitment(contact: AskedContact?) {
        contact?.let {
            ModalActivity.launchActivity(this, AddCommitmentFragment.newInstance(contact.appealId, contact.contactId))
        }
    }

    override fun onDestroy() {
        cleanupViewPager()
        super.onDestroy()
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel: AppealDetailsActivityDataModel by viewModels()

    private fun initDataModel() {
        dataModel.appealId.value = appealId
    }
    // endregion Data Model

    // region Data Binding
    override fun layoutId() = R.layout.activity_appeals_detail
    override lateinit var binding: ActivityAppealsDetailBinding

    override fun onCreateDataBinding(binding: ActivityAppealsDetailBinding) {
        super.onCreateDataBinding(binding)
        dataModel.appeal.observe(this) { binding.appeal = it }
    }
    // endregion Data Binding

    // region SwipeRefreshLayout logic
    private val swipeRefreshLayoutViewPagerHelper by lazy { SwipeRefreshLayoutViewPagerHelper() }

    private fun setupSwipeRefreshLayout() {
        swipeRefreshLayoutViewPagerHelper.swipeRefreshLayout = binding.refresh
        binding.refresh.setOnRefreshListener { dataModel.syncData(true) }
        dataModel.syncTracker.isSyncing.observe(this) { binding.refresh.isRefreshing = it }
    }
    // endregion SwipeRefreshLayout logic

    // region ViewPager logic
    private val adapter: AppealDetailsPagerAdapter by lazy { AppealDetailsPagerAdapter(this, supportFragmentManager) }

    private fun setupViewPager() {
        binding.details.also { pager ->
            pager.setScroller(Scroller(pager.context, DecelerateInterpolator()))
            pager.adapter = adapter
            pager.addOnPageChangeListener(swipeRefreshLayoutViewPagerHelper)
        }
        binding.appealsTabLayout.setupWithViewPager(binding.details)
    }

    private fun cleanupViewPager() {
        binding.appealsTabLayout.setupWithViewPager(null)
    }
    // endregion ViewPager logic

    // region Add Asked Contact/Commitment logic
    private fun showAddDialog() {
        AlertDialog.Builder(this)
            .setPositiveButton(R.string.appeals_add_commitment_button) { _, _ -> showAddCommitmentFragment() }
            .setNeutralButton(R.string.appeals_add_contact_to_appeal_button) { _, _ -> showContactSelector() }
            .show()
    }

    private fun showAddCommitmentFragment() {
        ModalActivity.launchActivity(this, AddCommitmentFragment.newInstance(appealId))
    }

    private fun showContactSelector() {
        appealId?.let { AddContactSelectorFragment(it).show(supportFragmentManager, R.id.root) }
    }

    override fun onItemSelected(item: Contact?) {
        val contactId = item?.id ?: return

        if (dataModel.excludedAppealContacts.value?.any { it.contact?.id == contactId } == true) {
            ConfirmExcludedContactDialogFragment(contactId).show(supportFragmentManager, null)
            return
        }

        saveAskedContact(contactId)
    }

    internal fun saveAskedContact(contactId: String, forceDeletion: Boolean = false) =
        appealsRepository.addAskedContact(appealId, contactId, forceDeletion)
    // endregion Add Asked Contact/Commitment logic

    override fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        super.setupToolbar()
    }

    override fun getPageName() = adapter.getPageTitle(binding.details.currentItem)
}

internal class ConfirmExcludedContactDialogFragment() : DialogFragment() {
    constructor(contactId: String) : this() {
        this.contactId = contactId
    }

    private var contactId by arg<String>()

    private val parent get() = activity as? AppealDetailsActivity
    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
        .setTitle(R.string.confirm)
        .setMessage(R.string.exclude_contact_message)
        .setPositiveButton(R.string.ok) { _, _ -> parent?.saveAskedContact(contactId, true) }
        .setNegativeButton(R.string.cancel, null).create()
}

@HiltViewModel
internal class AppealDetailsActivityDataModel @Inject constructor(
    private val appealsSyncService: AppealsSyncService,
    private val askedContactsSyncService: AskedContactsSyncService,
    private val excludedAppealContactsSyncService: ExcludedAppealContactsSyncService,
    private val pledgesSyncService: PledgesSyncService
) : RealmViewModel() {
    val appealId = MutableLiveData<String?>()

    private val distinctAppealId = appealId.distinctUntilChanged()
    val appeal = distinctAppealId.switchMap { realm.getAppeal(it).firstAsLiveData() }
    val askedContacts by lazy {
        distinctAppealId.switchMap { realm.getAskedContacts().forAppeal(it).hasName().sortByName().asLiveData() }
    }
    val pledgesCommitted by lazy {
        distinctAppealId.switchMap {
            realm.getPledges().forAppeal(it).status(Pledge.STATUS_COMMITTED).sortByName().asLiveData()
        }
    }
    val pledgesReceivedNotProcessed by lazy {
        distinctAppealId.switchMap {
            realm.getPledges().forAppeal(it).status(Pledge.STATUS_RECEIVED_NOT_PROCESSED).sortByName().asLiveData()
        }
    }
    val donationsGiven by lazy {
        distinctAppealId.switchMap { realm.getDonations().forAppeal(it).sortByDate().asLiveData() }
    }

    val excludedAppealContacts = appeal.switchMap { it?.excludedContacts?.asLiveData().orEmpty() }

    // region Sync Logic
    val syncTracker = SyncTracker()

    init {
        distinctAppealId.observe(this) { syncData() }
        appeal.map { it?.accountList?.id }.distinctUntilChanged().observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val appealId = distinctAppealId.value ?: return
        syncTracker.runSyncTasks(
            appealsSyncService.syncAppeal(appealId, force),
            askedContactsSyncService.syncAskedContacts(appealId, force),
            excludedAppealContactsSyncService.syncExcludedContactsFor(appealId, force)
        )

        val accountListId = appeal.value?.accountList?.id ?: return
        syncTracker.runSyncTask(
            pledgesSyncService.syncPledges(accountListId, appealId, force)
        )
    }
    // endregion Sync Logic
}
