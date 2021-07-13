package org.mpdx.android.features.coaching.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import androidx.annotation.MainThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import io.realm.Realm
import javax.inject.Inject
import javax.inject.Singleton
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.mpdx.android.R
import org.mpdx.android.base.AppConstantListener
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.core.realm.RealmLocked
import org.mpdx.android.core.realm.RealmManager
import org.mpdx.android.core.realm.RealmManagerEvent
import org.mpdx.android.core.realm.RealmUnlocked
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.MainActivity
import org.mpdx.android.features.base.BaseActivity
import org.mpdx.android.features.coaching.realm.getCoachingAccountLists
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.TemporalAdjusters
import timber.log.Timber

private const val TAG = "CoachingNotifManager"

private const val CHANNEL_ID_COACHING_REMINDER = "coaching_reminder"

private val CHANNELS_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

private const val NOTIFICATIONS_TAG = "coaching"
private const val NOTIFICATION_REMINDER_ID = 1

private const val PREFS_FILE = "coaching_notification_manager_state"
private const val PREF_HAS_COACHEES = "has_coachees"
private const val PREF_LAST_COACHING_REMINDER = "coaching_lastReminder"

@Singleton
class CoachingNotificationManager @Inject constructor(
    private val context: Context,
    private val appPrefs: AppPrefs,
    eventBus: EventBus,
    realmManager: RealmManager,
    appConstantListener: AppConstantListener
) {

    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    init {
        eventBus.register(this)
    }

    val ACTION_REMINDER_NOTIFICATION_ALARM =
        "${appConstantListener.appID()}.ACTION_COACHING_REMINDER_NOTIFICATION_ALARM"

    fun updateChannels() {
        updateNotificationChannelCoachingReminder(onlyIfExists = true)
    }

    fun rescheduleNotifications() {
        scheduleNextReminderNotification()
    }

    // region Preferences

    private val prefs = context.getSharedPreferences(PREFS_FILE, MODE_PRIVATE)
    private var hasCoachees: Boolean
        get() = prefs.getBoolean(PREF_HAS_COACHEES, false)
        set(value) {
            if (hasCoachees == value) return
            prefs.edit().putBoolean(PREF_HAS_COACHEES, value).apply()
            if (value) scheduleNextReminderNotification() else cancelReminderNotification()
        }
    private var lastReminderTime: Instant?
        get() = prefs.getLong(PREF_LAST_COACHING_REMINDER, 0).takeUnless { it == 0L }
            ?.let { Instant.ofEpochMilli(it) } ?: Instant.now().also { lastReminderTime = it }
        set(value) = prefs.edit().apply {
            if (value != null) putLong(PREF_LAST_COACHING_REMINDER, value.toEpochMilli())
            else remove(PREF_LAST_COACHING_REMINDER)
        }.apply()

    // endregion Preferences

    // region Channels

    private fun updateNotificationChannelCoachingReminder(onlyIfExists: Boolean = false) {
        if (CHANNELS_SUPPORTED) {
            val manager: NotificationManager = context.getSystemService(NotificationManager::class.java) ?: return
            if (onlyIfExists) manager.getNotificationChannel(CHANNEL_ID_COACHING_REMINDER) ?: return

            // create/update the notification channel
            val channel = NotificationChannel(
                CHANNEL_ID_COACHING_REMINDER, context.getString(R.string.coaching_notification_channel_weekly_reminder),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }

    // endregion Channels

    // region Notifications

    private val reminderNotificationIntent
        get() = Intent(context, MainActivity::class.java)
            .putExtra(BaseActivity.EXTRA_DEEP_LINK_TYPE, BaseActivity.DEEP_LINK_TYPE_COACHING)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .let { intent ->
                TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(intent)
                    .getPendingIntent(-1, 0)
            }

    internal fun triggerReminderNotification() {
        if (!hasCoachees || !appPrefs.hasCoachWeeklyNotification) return

        updateNotificationChannelCoachingReminder()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COACHING_REMINDER)
            .setContentTitle(context.getString(R.string.coaching_weekly_notification))
            .setSmallIcon(R.drawable.mpdx_logo_notification_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(reminderNotificationIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATIONS_TAG, NOTIFICATION_REMINDER_ID, notification)
        Timber.tag(TAG).d("Coaching Notification triggered at %s", Instant.now())
        lastReminderTime = Instant.now()

        scheduleNextReminderNotification()
    }

    // endregion Notifications

    // region Scheduling

    init {
        appPrefs.hasCoachWeeklyNotificationLiveData.observeForever { enabled ->
            if (enabled) scheduleNextReminderNotification() else cancelReminderNotification()
        }
    }

    private val reminderAlarmIntent
        get() = Intent(context, CoachingNotificationReceiver::class.java)
            .setAction(ACTION_REMINDER_NOTIFICATION_ALARM)
            .let { PendingIntent.getBroadcast(context, -1, it, 0) }
    private val nextReminderTime: Instant
        get() = lastReminderTime!!.atZone(ZoneId.systemDefault())
            .run {
                if (dayOfWeek != DayOfWeek.FRIDAY || hour > 11) with(TemporalAdjusters.next(DayOfWeek.FRIDAY)) else this
            }
            .withHour(12).withMinute(0).withSecond(0).withNano(0)
            .toInstant()

    private fun scheduleNextReminderNotification() {
        if (!hasCoachees || !appPrefs.hasCoachWeeklyNotification) return

        val time = nextReminderTime
        if (time.toEpochMilli() < System.currentTimeMillis()) {
            triggerReminderNotification()
            return
        }

        alarmManager?.set(AlarmManager.RTC, time.toEpochMilli(), reminderAlarmIntent)
        Timber.tag(TAG).d("Coaching Notification scheduled for %s", time)
    }

    private fun cancelReminderNotification() {
        lastReminderTime = null
        alarmManager?.cancel(reminderAlarmIntent)
        Timber.tag(TAG).d("Coaching Notification canceled")
    }

    // endregion Scheduling

    // region Data Monitoring

    private var coachingMonitor: CoachingMonitor? = if (realmManager.isUnlocked) CoachingMonitor() else null
        set(value) {
            if (value == field) return
            field?.destroy()
            field = value
        }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRealmEvent(event: RealmManagerEvent) {
        when (event) {
            RealmLocked -> coachingMonitor = null
            RealmUnlocked -> coachingMonitor = CoachingMonitor()
        }
    }

    @MainThread
    private inner class CoachingMonitor {
        private val realm: Realm = Realm.getDefaultInstance()
        private val hasCoachees = realm.getCoachingAccountLists().asLiveData().let {
            Transformations.map(it) { results -> results?.takeIf { results.isLoaded }?.let { results.size > 0 } }
        }
        private val coacheeObserver =
            Observer<Boolean?> { if (it != null) this@CoachingNotificationManager.hasCoachees = it }

        init {
            hasCoachees.observeForever(coacheeObserver)
        }

        fun destroy() {
            hasCoachees.removeObserver(coacheeObserver)
            realm.close()
        }
    }

    // endregion Data Monitoring
}
