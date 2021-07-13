package org.mpdx.android.features.analytics.model

const val ACTION_CONTACT_CALL_CLICKED = "Contact_Call_Clicked"
const val ACTION_CONTACT_DIRECTIONS_CLICKED = "Contact_Directions_Clicked"
const val ACTION_CONTACT_EMAIL_CLICKED = "Contact_Email_Clicked"
const val ACTION_CONTACT_SMS_CLICKED = "Contact_SMS_Clicked"

const val ACTION_CRYPTO_FAILURE = "Crypto_Failure"

const val ACTION_FINGERPRINT_ENABLED = "Fingerprint_Enabled"
const val ACTION_FINGERPRINT_SCANNER = "Fingerprint_Scanner"

const val ACTION_LOCK_METHOD = "Lock_Method"

const val ACTION_TASK_DETAIL_ACTION = "Task_Detail_Action"

const val CATEGORY_CONTACTS = "Contacts"
const val CATEGORY_SETUP = "Setup"
const val CATEGORY_TASKS = "Tasks"

const val LABEL_PIN = "PIN"
const val LABEL_FINGERPRINT = "FINGERPRINT"

open class AnalyticsActionEvent @JvmOverloads constructor(
    val action: String,
    val category: String? = null,
    val label: String? = null
) : AnalyticsBaseEvent() {
    open val attributes: Map<String, Any>? get() = null
}
