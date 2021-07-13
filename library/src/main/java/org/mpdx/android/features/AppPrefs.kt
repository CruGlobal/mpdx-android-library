package org.mpdx.android.features

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.distinctUntilChanged
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import org.ccci.gto.android.common.androidx.lifecycle.getBooleanLiveData
import org.ccci.gto.android.common.androidx.lifecycle.getStringLiveData
import org.mpdx.android.base.AuthenticationListener

private const val PREFS_FILE = "mpdxSharedPrefs"

private const val KEY_USER_ID = "userId"
private const val KEY_FINGERPRINT_IV = "secure_iv"
private const val KEY_REALM_KEY_SECURED_WITH_PIN = "realm_key_pin"
private const val KEY_REALM_KEY_SECURED_WITH_FINGERPRINT = "realm_key_fingerprint"
private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
private const val KEY_ACCOUNT_LIST_ID = "account_list_id"
private const val KEY_INVALID_PIN_COUNT = "invalid_pin_count"
private const val KEY_NOTIFICATION_TASK_DUE_ENABLED = "notification_task_due_enabled"
private const val KEY_APP_STARTED = "app_started"
private const val KEY_APP_STARTED_CONTACT_ID = "app_started_contact_id"
private const val KEY_APP_STARTED_TIME = "app_started_time"
private const val KEY_COACH_WEEKLY_NOTIFICATION = "coach_weeklky_notification"
private const val KEY_LAST_EXPERIENCE_VOTE_CAST = "experience_vote_cast"

@Singleton
class AppPrefs @Inject constructor(
    @ApplicationContext context: Context,
    private val authorizationListener: AuthenticationListener
) {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val editor get() = prefs.edit()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(id) = editor.putString(KEY_USER_ID, id).apply()

    var realmKeySecuredWithPin: String?
        get() = prefs.getString(KEY_REALM_KEY_SECURED_WITH_PIN, null)
        set(encryptedRealmKey) = editor.putString(KEY_REALM_KEY_SECURED_WITH_PIN, encryptedRealmKey).apply()

    var realmKeySecuredWithFingerprint: String?
        get() = prefs.getString(KEY_REALM_KEY_SECURED_WITH_FINGERPRINT, null)
        set(encryptedRealmKey) = editor.putString(KEY_REALM_KEY_SECURED_WITH_FINGERPRINT, encryptedRealmKey).apply()

    var secureIv: String?
        get() = prefs.getString(KEY_FINGERPRINT_IV, null)
        set(secureIv) = editor.putString(KEY_FINGERPRINT_IV, secureIv).apply()

    var accountListId: String?
        get() = prefs.getString(KEY_ACCOUNT_LIST_ID, null)
        set(id) = editor.putString(KEY_ACCOUNT_LIST_ID, id).apply()
    val accountListIdLiveData by lazy { prefs.getStringLiveData(KEY_ACCOUNT_LIST_ID, null).distinctUntilChanged() }

    var isTaskDueNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_TASK_DUE_ENABLED, true)
        set(state) = editor.putBoolean(KEY_NOTIFICATION_TASK_DUE_ENABLED, state).apply()
    val isTaskDueNotificationEnabledLiveData by lazy {
        prefs.getBooleanLiveData(KEY_NOTIFICATION_TASK_DUE_ENABLED, true).distinctUntilChanged()
    }

    val invalidKeyCount: Int
        get() = prefs.getInt(KEY_INVALID_PIN_COUNT, 0)

    var lastStartedAppId: String?
        get() = prefs.getString(KEY_APP_STARTED_CONTACT_ID, null)
        set(id) = editor.putString(KEY_APP_STARTED_CONTACT_ID, id).apply()

    var appStartedTime: Date?
        get() = if (prefs.contains(KEY_APP_STARTED_TIME)) Date(prefs.getLong(KEY_APP_STARTED_TIME, 0)) else null
        set(value) = prefs.edit {
            if (value != null) putLong(KEY_APP_STARTED_TIME, value.time) else remove(
                KEY_APP_STARTED_TIME
            )
        }

    var hasCoachWeeklyNotification: Boolean
        get() = prefs.getBoolean(KEY_COACH_WEEKLY_NOTIFICATION, true)
        set(enabled) = editor.putBoolean(KEY_COACH_WEEKLY_NOTIFICATION, enabled).apply()
    val hasCoachWeeklyNotificationLiveData by lazy {
        prefs.getBooleanLiveData(KEY_COACH_WEEKLY_NOTIFICATION, true).distinctUntilChanged()
    }

    val lastExperienceVoteCast: Calendar
        get() {
            val calendar = GregorianCalendar()
            calendar.timeInMillis = prefs.getLong(
                KEY_LAST_EXPERIENCE_VOTE_CAST,
                GregorianCalendar(2000, Calendar.JANUARY, 1).timeInMillis
            )
            return calendar
        }

    fun getAndClearLastStartedApp(): String? {
        val value = prefs.getString(KEY_APP_STARTED, null)
        setLastStartedApp(null)
        return value
    }

    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun setCompletedOnboarding(isOnboardingComplete: Boolean) {
        editor.putBoolean(KEY_ONBOARDING_COMPLETE, isOnboardingComplete).apply()
    }

    fun incrementInvalidKeyCount() {
        editor.putInt(KEY_INVALID_PIN_COUNT, invalidKeyCount + 1).apply()
    }

    fun setLastStartedApp(lastStartedApp: String?) {
        editor.putString(KEY_APP_STARTED, lastStartedApp).apply()
        appStartedTime = Date()
    }

    fun setLastExperienceVoteCast() {
        editor.putLong(KEY_LAST_EXPERIENCE_VOTE_CAST, Calendar.getInstance(TimeZone.getDefault()).timeInMillis).apply()
    }

    fun resetInvalidKeyCount() {
        editor.putInt(KEY_INVALID_PIN_COUNT, 0).apply()
    }

    fun resetRealmKey() {
        setCompletedOnboarding(false)
        realmKeySecuredWithPin = null
        realmKeySecuredWithFingerprint = null
    }

    fun logoutUser(activity: Activity) {
        accountListId = null
        authorizationListener.logOutOfSession(activity)
    }

    // This is a temporary solution to removing SplashActivity reference from location that have trouble Injecting Listener
    fun getSplashActivityIntent(context: Context) = authorizationListener.getSplashActivityIntent(context)
}
