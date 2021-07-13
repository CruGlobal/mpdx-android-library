package org.mpdx.android.features.coaching.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.Intent.ACTION_LOCALE_CHANGED
import androidx.annotation.MainThread
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CoachingNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var coachingNotificationManager: CoachingNotificationManager

    @MainThread
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_BOOT_COMPLETED -> coachingNotificationManager.rescheduleNotifications()
            ACTION_LOCALE_CHANGED -> coachingNotificationManager.updateChannels()
            coachingNotificationManager.ACTION_REMINDER_NOTIFICATION_ALARM ->
                coachingNotificationManager.triggerReminderNotification()
        }
    }
}
