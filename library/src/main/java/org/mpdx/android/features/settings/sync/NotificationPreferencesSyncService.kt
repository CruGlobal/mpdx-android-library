package org.mpdx.android.features.settings.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.base.TimeConstants
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.ApiConstants.ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.api.util.perPage
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.settings.api.SettingsApi
import org.mpdx.android.features.settings.model.NotificationPreference
import org.mpdx.android.features.settings.realm.getDirtyNotificationPreferences
import org.mpdx.android.features.settings.realm.getNotificationPreferences
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "NotificationPrefSync"

private const val SUBTYPE_NOTIFICATION_PREFERENCES = "notification_preferences"
private const val SUBTYPE_DIRTY_NOTIFICATION_PREFERENCES = "dirty_notification_preferences"

private const val SYNC_KEY_NOTIFICATION_PREFERENCES = "notification_preferences"

private const val STALE_DURATION_NOTIFICATION_PREFERENCES = TimeConstants.HOUR_IN_MS

@Singleton
class NotificationPreferencesSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val settingsApi: SettingsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_NOTIFICATION_PREFERENCES -> syncNotificationPreferences(args)
            SUBTYPE_DIRTY_NOTIFICATION_PREFERENCES -> syncDirtyNotificationPreferences(args)
        }
    }

    // region NotificationPreferences sync

    private val notificationPreferencesMutex = MutexMap()
    private val params = JsonApiParams()
        .include(NotificationPreference.JSON_NOTIFICATION_TYPE)
        .perPage(100)

    private suspend fun syncNotificationPreferences(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return

        notificationPreferencesMutex.withLock(accountListId) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_NOTIFICATION_PREFERENCES, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_NOTIFICATION_PREFERENCES, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                val responses = fetchPages { p ->
                    settingsApi.getNotificationPreferencesAsync(accountListId, JsonApiParams().addAll(params).page(p))
                }

                realmTransaction {
                    val prefs = responses.aggregateData()
                    prefs.forEach { it.accountListId = accountListId }

                    saveInRealm(
                        prefs, getNotificationPreferences(accountListId).asExisting(),
                        deleteOrphanedExistingItems = !responses.hasErrors()
                    )
                    if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the notification preferences from the API")
            }
        }
    }

    fun syncNotificationPreferences(accountListId: String, force: Boolean = false) =
        Bundle().putAccountListId(accountListId).toSyncTask(SUBTYPE_NOTIFICATION_PREFERENCES, force)

    // endregion NotificationPreferences sync

    // region Dirty NotificationPreference sync

    private val dirtyNotificationPreferences = Mutex()
    private suspend fun syncDirtyNotificationPreferences(args: Bundle) = dirtyNotificationPreferences.withLock {
        coroutineScope {
            realm { copyFromRealm(getDirtyNotificationPreferences().findAll()) }
                .forEach { pref: NotificationPreference ->
                    launch {
                        when {
                            pref.isDeleted -> TODO("We currently don't support deleting NotificationPreferences")
                            pref.isNew -> TODO("We currently don't support creating NotificationPreferences")
                            pref.hasChangedFields -> syncChangedNotificationPreference(pref)
                        }
                    }
                }
        }
    }

    private suspend fun syncChangedNotificationPreference(preference: NotificationPreference) {
        val accountListId = preference.accountListId ?: return
        val prefId = preference.id ?: return

        try {
            settingsApi.updateNotificationPreferencesAsync(
                accountListId, prefId, params, createPartialUpdate(preference)
            )
                .onSuccess { body -> realmTransaction { saveInRealm(body.data) } }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // invalid updated_in_db_at time
                            code() == 409 && error.detail?.startsWith(ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX) == true -> {
                                syncNotificationPreferences(accountListId, true).run()
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error storing the changed notification preference %s to the API", prefId)
        }
    }

    fun syncDirtyNotificationPreferences() = Bundle().toSyncTask(SUBTYPE_DIRTY_NOTIFICATION_PREFERENCES)

    // endregion Dirty NotificationPreference sync
}
