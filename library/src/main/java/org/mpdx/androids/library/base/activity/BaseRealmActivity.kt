package org.mpdx.androids.library.base.activity

import android.os.Bundle
import com.karumi.weak.weak
import io.realm.Realm
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.mpdx.core.realm.RealmLocked

abstract class BaseRealmActivity : BaseActivity() {
    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        setupRealm()
    }

    override fun onDestroy() {
        cleanupRealm()
        super.onDestroy()
    }
    // endregion Lifecycle

    // region Realm
    protected lateinit var realm: Realm
        private set

    private var realmEventSubscriber: RealmEventBusSubscriber? = null
        set(value) {
            if (field == value) return
            if (field != null) mEventBus.unregister(field)
            field = value
            if (field != null) mEventBus.register(field)
        }

    internal class RealmEventBusSubscriber(activity: BaseRealmActivity) {
        private val activity: BaseRealmActivity? by weak(activity)

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onRealmLocked(event: RealmLocked) = activity?.realmLocked()
    }

    private inline fun setupRealm() {
        realmEventSubscriber = RealmEventBusSubscriber(this)
        openRealm()
    }

    private fun openRealm(): Boolean {
        if (mRealmManager.isUnlocked) {
            closeRealm()
            realm = Realm.getDefaultInstance()
            return true
        }

        finish()
        return false
    }

    private fun realmLocked() {
        closeRealm()
        openRealm()
    }

    private fun closeRealm() {
        if (::realm.isInitialized) realm.close()
    }

    private inline fun cleanupRealm() {
        realmEventSubscriber = null
        closeRealm()
    }
    // endregion Realm
}
