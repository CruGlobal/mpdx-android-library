package org.mpdx.android.features.notifications.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.filter.realm.getNotificationFilters
import org.mpdx.android.features.filter.realm.isEnabled
import org.mpdx.android.features.notifications.model.UserNotification
import org.mpdx.android.features.notifications.realm.applyFilters
import org.mpdx.android.features.notifications.realm.getUserNotifications
import org.mpdx.android.features.notifications.realm.isUnread
import org.mpdx.android.features.notifications.sync.NotificationsSyncService

@HiltViewModel
class NotificationsFragmentViewModel @Inject constructor(
    private val notificationsSyncService: NotificationsSyncService
) : RealmViewModel() {
    val unreadOnly = MutableLiveData(true)

    val filters by lazy { realm.getNotificationFilters().isEnabled().asLiveData() }

    val userNotifications: LiveData<RealmResults<UserNotification>> by lazy {
        unreadOnly.switchCombineWith(filters) { unreadOnly, filters ->
            realm.getUserNotifications()
                .apply { if (unreadOnly == true) isUnread() }
                .applyFilters(filters)
                .asLiveData()
        }
    }

    // region Sync logic
    val syncTracker = SyncTracker()
    fun syncData(force: Boolean = false) =
        syncTracker.runSyncTask(notificationsSyncService.syncNotifications(force))
    // endregion Sync logic
}
