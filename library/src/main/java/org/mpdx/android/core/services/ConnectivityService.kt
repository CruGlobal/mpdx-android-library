package org.mpdx.android.core.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Singleton
import org.greenrobot.eventbus.EventBus
import org.mpdx.android.utils.isNetworkAvailable

@Singleton
class ConnectivityService @Inject constructor(context: Context, private val eventBus: EventBus) {
    enum class ConnectivityEvent { ONLINE, OFFLINE }

    private var state = if (context.isNetworkAvailable) ConnectivityEvent.ONLINE else ConnectivityEvent.OFFLINE
        set(value) {
            if (field == value) return
            field = value
            eventBus.postSticky(value)
        }

    init {
        // TODO: utilize ConnectivityManager.registerNetworkCallback for API >= 21
        context.registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        eventBus.postSticky(state)
    }

    @AndroidEntryPoint
    class ConnectivityReceiver : BroadcastReceiver() {
        @Inject
        lateinit var connectivityService: ConnectivityService

        override fun onReceive(context: Context, intent: Intent) {
            connectivityService.state =
                if (context.isNetworkAvailable) ConnectivityEvent.ONLINE else ConnectivityEvent.OFFLINE
        }
    }
}
