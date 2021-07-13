package org.mpdx.android.features.donations.add

import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.fragment.app.findListener
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.filterByQuery
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.donations.model.DesignationAccount
import org.mpdx.android.features.donations.model.DesignationAccountFields
import org.mpdx.android.features.donations.realm.getDesignationAccounts
import org.mpdx.android.features.donations.realm.sortByName
import org.mpdx.android.features.donations.realm.withName
import org.mpdx.android.features.donations.sync.DesignationAccountsSyncService
import org.mpdx.android.features.selector.BaseSelectorFragment
import org.mpdx.android.features.selector.SelectorFragmentDataModel

@AndroidEntryPoint
class DesignationSelectorFragment : BaseSelectorFragment<DesignationAccount>(
    R.string.donation_add_donation_designation_select, DesignationAccount::displayName, true
) {
    override val dataModel: DesignationSelectorFragmentDataModel by viewModels()

    override fun dispatchItemSelectedCallback(item: DesignationAccount?) {
        findListener<OnDesignationSelectedListener>()?.onDesignationAccountSelected(item)
    }
}

@HiltViewModel
class DesignationSelectorFragmentDataModel @Inject constructor(
    appPrefs: AppPrefs,
    private val designationAccountsSyncService: DesignationAccountsSyncService
) : RealmViewModel(), SelectorFragmentDataModel<DesignationAccount> {
    private val accountListId = appPrefs.accountListIdLiveData
    override val query = MutableLiveData("")

    override val items = accountListId.switchCombineWith(query) { accountListId, query ->
        realm.getDesignationAccounts(accountListId)
            .withName()
            .filterByQuery(DesignationAccountFields.DISPLAY_NAME, query)
            .sortByName()
            .asLiveData()
    }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val accountListId = accountListId.value ?: return
        syncTracker.runSyncTasks(designationAccountsSyncService.syncDesignationAccounts(accountListId, force))
    }
    // endregion Sync logic
}

interface OnDesignationSelectedListener {
    fun onDesignationAccountSelected(designation: DesignationAccount?)
}
