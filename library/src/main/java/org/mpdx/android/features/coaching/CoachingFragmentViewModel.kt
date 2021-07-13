package org.mpdx.android.features.coaching

import androidx.lifecycle.LiveData
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import javax.inject.Inject
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.core.model.AccountListFields
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.coaching.realm.getCoachingAccountLists
import org.mpdx.android.features.coaching.sync.CoachingSyncService

@HiltViewModel
class CoachingFragmentViewModel @Inject constructor(
    appPrefs: AppPrefs,
    private val coachingSyncService: CoachingSyncService
) : RealmViewModel() {
    val allCoachingAccounts: LiveData<RealmResults<AccountList>> by lazy {
        appPrefs.accountListIdLiveData.switchMap {
            realm.getCoachingAccountLists().or().equalTo(AccountListFields.ID, it).asLiveData()
        }
    }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        syncData()
    }

    fun syncData(force: Boolean = false) {
        syncTracker.runSyncTask(coachingSyncService.syncAccountLists(force))
    }
    // endregion Sync logic
}
