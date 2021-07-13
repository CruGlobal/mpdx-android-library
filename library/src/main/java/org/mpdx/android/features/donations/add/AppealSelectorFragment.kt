package org.mpdx.android.features.donations.add

import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Sort
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
import org.mpdx.android.features.appeals.model.Appeal
import org.mpdx.android.features.appeals.model.AppealFields
import org.mpdx.android.features.appeals.realm.getAppeals
import org.mpdx.android.features.appeals.realm.hasName
import org.mpdx.android.features.appeals.sync.AppealsSyncService
import org.mpdx.android.features.selector.BaseSelectorFragment
import org.mpdx.android.features.selector.SelectorFragmentDataModel

@AndroidEntryPoint
class AppealSelectorFragment :
    BaseSelectorFragment<Appeal>(R.string.donation_add_donation_appeal_select, Appeal::name, true) {
    override val dataModel: AppealSelectorFragmentDataModel by viewModels()

    override fun dispatchItemSelectedCallback(item: Appeal?) {
        findListener<OnAppealSelectedListener>()?.onAppealSelected(item)
    }
}

@HiltViewModel
class AppealSelectorFragmentDataModel @Inject constructor(
    appPrefs: AppPrefs,
    private val appealsSyncService: AppealsSyncService
) : RealmViewModel(), SelectorFragmentDataModel<Appeal> {
    private val accountListId = appPrefs.accountListIdLiveData
    override val query = MutableLiveData("")

    override val items = accountListId.switchCombineWith(query) { accountListId, query ->
        realm.getAppeals(accountListId).hasName()
            .filterByQuery(AppealFields.NAME, query)
            .sort(AppealFields.CREATED_AT, Sort.DESCENDING)
            .asLiveData()
    }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData(false) }
    }

    fun syncData(force: Boolean = false) {
        val accountListId = accountListId.value ?: return
        syncTracker.runSyncTasks(appealsSyncService.syncAppeals(accountListId, force))
    }
    // endregion Sync logic
}

interface OnAppealSelectedListener {
    fun onAppealSelected(appeal: Appeal?)
}
