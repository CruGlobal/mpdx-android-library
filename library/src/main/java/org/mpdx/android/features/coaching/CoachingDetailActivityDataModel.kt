package org.mpdx.android.features.coaching

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.features.coaching.realm.getCoachingAnalytics
import org.mpdx.android.features.coaching.realm.getCoachingAppointmentResults
import org.mpdx.android.features.coaching.sync.CoachingSyncService
import org.threeten.bp.LocalDate

@HiltViewModel
class CoachingDetailActivityDataModel @Inject constructor(
    private val coachingSyncService: CoachingSyncService
) : RealmViewModel() {
    val accountListId = MutableLiveData<String?>()
    val range = MutableLiveData<ClosedRange<LocalDate>>()

    val accountList by lazy { accountListId.switchMap { realm.getAccountList(it).firstAsLiveData() } }

    val analytics = accountListId.switchCombineWith(range) { accountListId, range ->
        realm.getCoachingAnalytics(accountListId, range).firstAsLiveData()
    }

    val appointments = accountListId.switchCombineWith(range) { accountListId, range ->
        realm.getCoachingAppointmentResults(accountListId, range).firstAsLiveData()
    }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
        range.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val accountListId = accountListId.value ?: return
        val range = range.value ?: return

        syncTracker.runSyncTasks(
            coachingSyncService.syncAnalytics(accountListId, range, force),
            coachingSyncService.syncAppointmentResults(accountListId, range, force)
        )
    }
    // endregion Sync logic
}
