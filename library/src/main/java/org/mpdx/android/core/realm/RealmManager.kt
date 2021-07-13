package org.mpdx.android.core.realm

import io.realm.DefaultCompactOnLaunchCallback
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.exceptions.RealmFileException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.mpdx.android.base.AppConstantListener
import org.mpdx.android.core.data.realm.MpdxRealmMigration
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "RealmManager"

private val lockedConfiguration = RealmConfiguration.Builder().inMemory().build()
private val compactOnLaunchCallback by lazy { DefaultCompactOnLaunchCallback() }

@Singleton
class RealmManager @Inject constructor(
    private val eventBus: EventBus,
    private val migration: MpdxRealmMigration,
    private val appConstantListener: AppConstantListener
) {
    @Volatile
    var isUnlocked = false
        private set

    init {
        lockRealm()
    }

    @Synchronized
    private fun lockRealm() {
        isUnlocked = false
        Realm.setDefaultConfiguration(lockedConfiguration)
        eventBus.post(RealmLocked)
    }

    @Synchronized
    fun unlockRealm(key: ByteArray): Boolean {
        if (isUnlocked && Realm.getDefaultConfiguration()?.encryptionKey?.contentEquals(key) == true) return true

        val config = RealmConfiguration.Builder()
            // TODO: Realm upgrade may cause crashes this is to prevent them in release build
            .allowWritesOnUiThread(!appConstantListener.isDebug())
            .encryptionKey(key)
            .schemaVersion(MpdxRealmMigration.VERSION)
            .migration(migration)
            .apply { if (!appConstantListener.isDebug()) deleteRealmIfMigrationNeeded() }
            .compactOnLaunch(compactOnLaunchCallback)
            .build()
        Realm.setDefaultConfiguration(config)

        return try {
            realm { isUnlocked = true }
            eventBus.post(RealmUnlocked)
            true
        } catch (e: RealmFileException) {
            Timber.tag(TAG).e(e, "Error unlocking Realm")
            lockRealm()
            false
        } catch (e: IllegalArgumentException) {
            lockRealm()
            false
        }
    }

    fun deleteRealm(lock: Boolean = false): Boolean {
        try {
            // attempt deleting file directly first
            try {
                if (Realm.deleteRealm(RealmConfiguration.Builder().build())) return true
            } catch (e: IllegalStateException) {
                Timber.tag(TAG).d(e, "Unable to delete realm file")
            }

            // attempt deleting all data from the unlocked realm instance
            if (isUnlocked) {
                runBlocking {
                    withContext(Dispatchers.IO) { realmTransaction { deleteAll() } }
                }
                return true
            }

            return false
        } finally {
            if (lock) lockRealm()
        }
    }
}

sealed class RealmManagerEvent
object RealmLocked : RealmManagerEvent()
object RealmUnlocked : RealmManagerEvent()
