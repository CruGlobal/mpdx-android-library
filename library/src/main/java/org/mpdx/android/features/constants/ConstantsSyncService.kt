package org.mpdx.android.features.constants

import android.os.Bundle
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.ccci.gto.android.common.base.TimeConstants.WEEK_IN_MS
import org.ccci.gto.android.common.compat.util.LocaleCompat
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.constants.api.ConstantsApi
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "ConstantsSyncService"
private const val SYNC_ID = "constants"
private const val STALE_DURATION_SYNC = WEEK_IN_MS

@Singleton
class ConstantsSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val constantsApi: ConstantsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    private val syncMutex: Mutex = Mutex()

    override suspend fun sync(args: Bundle) {
        val locale = args.getLocale() ?: Locale.getDefault() ?: return
        syncMutex.withLock {
            val lastSyncTime = getLastSyncTime(SYNC_ID, LocaleCompat.toLanguageTag(locale))
                .also { if (!it.needsSync(STALE_DURATION_SYNC, args.isForced())) return }

            // execute the sync
            lastSyncTime.trackSync()
            try {
                constantsApi.getConstants(locale).onSuccess { body ->
                    body.dataSingle?.let { constants ->
                        constants.locale = locale
                        realmTransaction {
                            saveInRealm(constants)
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
                    withContext(Dispatchers.Main) { Constants.reload() }
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the constants list from the API")
            }
        }
    }

    fun sync(locale: Locale = Locale.getDefault(), force: Boolean = false) = Bundle()
        .putLocale(locale)
        .toSyncTask(force = force)
}
