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
import org.mpdx.android.core.realm.getDonorAccounts
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.contacts.model.DonorAccount
import org.mpdx.android.features.contacts.model.DonorAccountFields
import org.mpdx.android.features.contacts.sync.DonorAccountSyncService
import org.mpdx.android.features.donations.model.DesignationAccountFields
import org.mpdx.android.features.selector.BaseSelectorFragment
import org.mpdx.android.features.selector.SelectorFragmentDataModel

@AndroidEntryPoint
class DonorSelectorFragment : BaseSelectorFragment<DonorAccount>(
    R.string.donation_add_donation_details_select_donor, DonorAccount::displayName, true
) {
    override val dataModel: DonorSelectorFragmentDataModel by viewModels()

    override fun dispatchItemSelectedCallback(item: DonorAccount?) {
        findListener<OnDonorSelectedListener>()?.onDonorSelected(item)
    }
}

@HiltViewModel
class DonorSelectorFragmentDataModel @Inject constructor(
    appPrefs: AppPrefs,
    private val donorAccountSyncService: DonorAccountSyncService
) : RealmViewModel(), SelectorFragmentDataModel<DonorAccount> {
    private val accountListId = appPrefs.accountListIdLiveData
    override val query = MutableLiveData("")

    override val items = accountListId.switchCombineWith(query) { accountListId, query ->
        realm.getDonorAccounts(accountListId)
            .filterByQuery(DesignationAccountFields.DISPLAY_NAME, query)
            .sort(DonorAccountFields.DISPLAY_NAME)
            .asLiveData()
    }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val accountListId = accountListId.value ?: return
        syncTracker.runSyncTasks(donorAccountSyncService.syncDonorAccounts(accountListId, force))
    }
    // endregion Sync logic
}

interface OnDonorSelectedListener {
    fun onDonorSelected(partner: DonorAccount?)
}
