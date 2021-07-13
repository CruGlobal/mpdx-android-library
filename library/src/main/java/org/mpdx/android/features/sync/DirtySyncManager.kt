package org.mpdx.android.features.sync

import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.mpdx.android.base.AuthenticationListener
import org.mpdx.android.core.realm.RealmManager
import org.mpdx.android.core.realm.RealmUnlocked
import org.mpdx.android.core.services.ConnectivityService

// TODO: Remove Key Reference
@Singleton
class DirtySyncManager @Inject constructor(
    eventBus: EventBus,
    private val authenticationListener: AuthenticationListener,
    private val realmManager: RealmManager,
    private val batchSyncService: Lazy<BatchSyncService>
) {
    init {
        eventBus.register(this)
        triggerDirtySync()
    }

    @Subscribe(sticky = true)
    fun onConnectivityEvent(event: ConnectivityService.ConnectivityEvent) {
        if (event == ConnectivityService.ConnectivityEvent.ONLINE) triggerDirtySync()
    }

    @Subscribe
    fun onRealmUnlockedEvent(event: RealmUnlocked) = triggerDirtySync()

    fun triggerDirtySync() {
        if (!realmManager.isUnlocked) return
        if (authenticationListener.getSessionGuid() == null) return

        batchSyncService.get().syncDirtyData().launch()
    }
}
