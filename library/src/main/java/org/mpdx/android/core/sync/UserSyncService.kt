package org.mpdx.android.core.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.base.TimeConstants.HOUR_IN_MS
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.core.UserApi
import org.mpdx.android.core.model.User
import org.mpdx.android.core.model.asDbUser
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "UserSyncService"

private const val SUBTYPE_USER = "user"

private const val SYNC_KEY_USER = "user"

private const val STALE_DURATION_USER = 6 * HOUR_IN_MS

@Singleton
class UserSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val userApi: UserApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_USER -> syncUser(args)
        }
    }

    // region User sync
    private val userMutex = Mutex()
    private suspend fun syncUser(args: Bundle) {
        userMutex.withLock {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_USER)
                .also { if (!it.needsSync(STALE_DURATION_USER, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                userApi.getUser()
                    .onSuccess { body ->
                        val user = body.dataSingle
                        if (user != null) realmTransaction {
                            saveInRealm(user)
                            saveInRealm(user.asPerson())
                            saveInRealm(user.asDbUser())
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the user from the API")
            }
        }
    }

    fun syncUser(force: Boolean = false) = Bundle().toSyncTask(SUBTYPE_USER, force)
    // endregion User sync
}

private fun User.asPerson() = Person().also {
    it.id = id
    it.isPlaceholder = true
    it.replacePlaceholder = true
    it.firstName = firstName
    it.lastName = lastName
    it.avatarUrl = avatar
}
