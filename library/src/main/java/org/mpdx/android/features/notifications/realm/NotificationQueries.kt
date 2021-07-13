package org.mpdx.android.features.notifications.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.oneOf
import io.realm.kotlin.where
import org.mpdx.android.features.filter.model.Filter
import org.mpdx.android.features.notifications.model.NotificationTypeFields
import org.mpdx.android.features.notifications.model.UserNotification
import org.mpdx.android.features.notifications.model.UserNotificationFields

fun Realm.getUserNotifications() = where<UserNotification>()

fun Realm.getUserNotification(id: String?): RealmQuery<UserNotification> =
    where<UserNotification>().equalTo(UserNotificationFields.ID, id)

fun RealmQuery<UserNotification>.isUnread(): RealmQuery<UserNotification> =
    equalTo(UserNotificationFields.IS_READ, false)

fun RealmQuery<UserNotification>.applyFilters(filters: List<Filter>?) = apply {
    if (filters.isNullOrEmpty()) return@apply

    beginGroup()
    filters.groupBy { it.type }.forEach { (type, filters) ->
        when (type) {
            Filter.Type.NOTIFICATION_TYPES -> oneOf(
                "${UserNotificationFields.NOTIFICATION.NOTIFICATION_TYPE}.${NotificationTypeFields.ID}",
                filters.mapNotNull { it.key }.toTypedArray()
            )
            else -> Unit
        }
    }
    endGroup()
}
