package org.mpdx.android.features.notifications.list

import org.mpdx.android.features.notifications.model.UserNotification

interface OnNotificationSelectedListener {
    fun onNotificationSelected(notification: UserNotification?)
}
