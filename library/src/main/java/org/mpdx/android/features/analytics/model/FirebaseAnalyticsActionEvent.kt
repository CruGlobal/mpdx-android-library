package org.mpdx.android.features.analytics.model

import org.mpdx.android.features.analytics.AnalyticsSystem

private const val NAME_CONTACT_NEW_CLICK = "new_contact_clicked"

const val NAME_CHANGE_ACCOUNT = "change_account"
const val NAME_LOG_OUT = "logout"

private const val NAME_DONATION_NEW_CLICK = "new_donation_clicked"

private const val NAME_FILTER_APPLIED = "filter_applied"

private const val NAME_ICON_CALL = "call_icon"
private const val NAME_ICON_TEXT = "text_icon"
private const val NAME_ICON_EMAIL = "email_icon"
private const val NAME_ICON_DIRECTION = "directions_icon"

private const val NAME_AUTO_LOG_ADD_INFO = "auto_log_add_info"
private const val NAME_AUTO_LOG_CANCEL = "auto_log_cancel"
private const val NAME_AUTO_LOG_OK = "auto_log_ok"
private const val NAME_CHECK_MARK_CLICKED = "complete_checkmark_clicked"
private const val NAME_LOG_TASK = "task_log"
private const val NAME_TASK_DELETED = "task_deleted"
private const val NAME_TASK_NEW_CLICK = "new_task_clicked"
private const val NAME_TASK_NEW_LOG_CLICKED = "log_task_clicked"

// TODO:  This class is deeply embedded in many places and may need to be renamed.
sealed class FirebaseAnalyticsActionEvent(eventName: String) : AnalyticsActionEvent(eventName) {
    override fun isForSystem(system: AnalyticsSystem) = system == AnalyticsSystem.FIREBASE
}

object ContactNewClickAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_CONTACT_NEW_CLICK)

object DonationNewClickAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_DONATION_NEW_CLICK)

object FilterAppliedAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_FILTER_APPLIED)

object IconCallAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_ICON_CALL)
object IconTextAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_ICON_TEXT)
object IconEmailAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_ICON_EMAIL)
object IconDirectionAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_ICON_DIRECTION)

// region Settings
object SettingsChangeAccountAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_CHANGE_ACCOUNT)
object SettingsLogOutAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_LOG_OUT)
// endregion Settings

// region Task
object TaskAutoLogAddInfoAction : FirebaseAnalyticsActionEvent(NAME_AUTO_LOG_ADD_INFO)
object TaskAutoLogCancelAction : FirebaseAnalyticsActionEvent(NAME_AUTO_LOG_CANCEL)
object TaskAutoLogOkAction : FirebaseAnalyticsActionEvent(NAME_AUTO_LOG_OK)
object TaskCheckMarkClickedAction : FirebaseAnalyticsActionEvent(NAME_CHECK_MARK_CLICKED)
object TaskLogAction : FirebaseAnalyticsActionEvent(NAME_LOG_TASK)
object TaskDeletedAction : FirebaseAnalyticsActionEvent(NAME_TASK_DELETED)
object TaskNewClickAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_TASK_NEW_CLICK)
object TaskNewLogClickAnalyticsEvent : FirebaseAnalyticsActionEvent(NAME_TASK_NEW_LOG_CLICKED)
