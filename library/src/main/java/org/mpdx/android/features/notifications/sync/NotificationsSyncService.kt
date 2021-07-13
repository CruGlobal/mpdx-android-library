package org.mpdx.android.features.notifications.sync

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
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_CREATED_AT
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.api.util.perPage
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.JSON_API_TYPE_CONTACT
import org.mpdx.android.features.notifications.api.NotificationsApi
import org.mpdx.android.features.notifications.model.Notification
import org.mpdx.android.features.notifications.model.UserNotification
import org.mpdx.android.features.notifications.realm.getUserNotifications
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "NotificationSyncService"

private const val SUBTYPE_NOTIFICATIONS = "notifications"
private const val SUBTYPE_NOTIFICATIONS_FILTERED = "notifications_filtered"
private const val SUBTYPE_NOTIFICATIONS_DIRTY = "notifications_dirty"

private const val SYNC_KEY_NOTIFICATIONS = "notifications"
private const val SYNC_KEY_NOTIFICATIONS_FILTERED = "notifications_filtered"

private const val STALE_DURATION_NOTIFICATIONS = TimeConstants.DAY_IN_MS
private const val STALE_DURATION_NOTIFICATIONS_FILTERED = TimeConstants.HOUR_IN_MS

private const val PAGE_SIZE = 100

@Singleton
class NotificationsSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val notificationsApi: NotificationsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    private val notificationsMutex = Mutex()
    private val notificationsFilteredMutex = MutexMap()

    private val notificationParams = JsonApiParams()
        .include("${UserNotification.JSON_NOTIFICATION}.${Notification.JSON_NOTIFICATION_TYPE}")
        .include("${UserNotification.JSON_NOTIFICATION}.${Notification.JSON_CONTACT}")
        .fields(JSON_API_TYPE_CONTACT, *Contact.JSON_FIELDS_SPARSE)
        .sort("-$JSON_ATTR_CREATED_AT")
        .perPage(PAGE_SIZE)

    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_NOTIFICATIONS -> syncNotifications(args)
            SUBTYPE_NOTIFICATIONS_FILTERED -> syncNotificationsFiltered(args)
            SUBTYPE_NOTIFICATIONS_DIRTY -> syncDirtyNotifications(args)
        }
    }

    // region Notifications sync
    private suspend fun syncNotifications(args: Bundle) = notificationsMutex.withLock {
        val lastSyncTime = getLastSyncTime(SYNC_KEY_NOTIFICATIONS)
            .also { if (!it.needsSync(STALE_DURATION_NOTIFICATIONS, args.isForced())) return }

        lastSyncTime.trackSync()
        try {
            val responses = fetchPages(pages = 7) { page ->
                notificationsApi.getNotifications(JsonApiParams().addAll(notificationParams).page(page))
            }

            realmTransaction {
                saveInRealm(
                    responses.aggregateData().markPlaceholders(), getUserNotifications().asExisting(),
                    deleteOrphanedExistingItems = !responses.hasErrors()
                )
                if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
            }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error retrieving the user notifications from the API")
        }
    }

    fun syncNotifications(force: Boolean = false) = Bundle().toSyncTask(SUBTYPE_NOTIFICATIONS, force)
    // endregion Notifications sync

    // TODO: this isn't used or complete yet
    private suspend fun syncNotificationsFiltered(args: Bundle) {
        val page = args.getPage()
        if (page < 1) return

        notificationsFilteredMutex.withLock(page) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_NOTIFICATIONS_FILTERED, page.toString())
                .also { if (!it.needsSync(STALE_DURATION_NOTIFICATIONS_FILTERED, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                notificationsApi.getNotifications(JsonApiParams().addAll(notificationParams).page(page))
                    .onSuccess { body ->
                        realmTransaction {
                            saveInRealm(body.data)
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving page %d of notifications from the API", page)
            }
        }
    }

    // region Dirty Notification sync
    private val dirtyNotificationsMutex = Mutex()
    private suspend fun syncDirtyNotifications(args: Bundle) = dirtyNotificationsMutex.withLock {
        coroutineScope {
            realm { copyFromRealm(getUserNotifications().isDirty().findAll()) }
                .forEach { notification: UserNotification ->
                    launch {
                        when {
                            notification.isDeleted -> TODO("We currently don't support deleting Notifications")
                            notification.isNew -> TODO("We currently don't support creating Notifications on Android")
                            notification.hasChangedFields -> syncChangedNotification(notification)
                        }
                    }
                }
        }
    }

    private suspend fun syncChangedNotification(notification: UserNotification) {
        val notificationId = notification.id ?: return
        try {
            notificationsApi.updateNotification(
                notificationId, notificationParams, createPartialUpdate(notification)
            )
                .onSuccess { realmTransaction { saveInRealm(it.data.markPlaceholders()) } }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // invalid updated_in_db_at time
                            code() == 409 && error.detail?.startsWith(ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX) == true -> {
                                // TODO: trigger a sync for an individual notification only
                                syncNotifications().launch()
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unhandled JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error storing the changed notification %s to the API", notificationId)
        }
        return
    }

    fun syncDirtyNotifications() = Bundle().toSyncTask(SUBTYPE_NOTIFICATIONS_DIRTY)
    // endregion Dirty Notification sync

    private fun List<UserNotification>.markPlaceholders(): List<UserNotification> = onEach {
        it.notification?.contact?.apply {
            isPlaceholder = true
            replacePlaceholder = true
        }
    }
}
