package org.mpdx.android.features.donations.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import io.realm.Sort
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.donations.model.Donation
import org.mpdx.android.features.donations.model.DonationFields
import org.mpdx.android.features.donations.realm.forAccountList
import org.mpdx.android.features.donations.realm.forMonth
import org.mpdx.android.features.donations.realm.getDonations
import org.mpdx.android.features.donations.sync.DonationsSyncService
import org.threeten.bp.YearMonth

@HiltViewModel
class DonationsFragmentViewModel @Inject constructor(
    appPrefs: AppPrefs,
    private val donationsSyncService: DonationsSyncService
) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData
    val currentMonth = MutableLiveData<YearMonth>(YearMonth.now())

    private val donationsByMonth = mutableMapOf<YearMonth?, LiveData<RealmResults<Donation>>>()
    fun getDonationsFor(month: YearMonth): LiveData<RealmResults<Donation>> = donationsByMonth.getOrPut(month) {
        accountListId.switchMap {
            realm.getDonations().forAccountList(it).forMonth(month)
                .sort(DonationFields.DONATION_DATE, Sort.DESCENDING)
                .asLiveData()
        }
    }

    val donations by lazy { currentMonth.switchMap { getDonationsFor(it ?: YearMonth.now()) } }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
        currentMonth.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val accountListId = accountListId.value ?: return
        val month = currentMonth.value ?: return
        syncTracker.runSyncTask(donationsSyncService.syncDonations(accountListId, month, force))
    }
    // endregion Sync logic
}
