package org.mpdx.android.features.analytics.model

const val SCREEN_APPEALS_ASKED = "mpdx : appeals : asked"
const val SCREEN_APPEALS_COMMITTED = "mpdx : appeals : committed"
const val SCREEN_APPEALS_GIVEN = "mpdx : appeals : given"
const val SCREEN_APPEALS_LIST = "mpdx : appeals : list"
const val SCREEN_APPEALS_RECEIVED = "mpdx : appeals : received"

const val SCREEN_COACHING_LIST = "mpdx : coaching : list"
const val SCREEN_COACHING_DETAIL_MONTH = "mpdx : coaching : details : month"
const val SCREEN_COACHING_DETAIL_WEEK = "mpdx : coaching : details : week"

const val SCREEN_CONTACTS = "mpdx : contacts : list"
const val SCREEN_CONTACTS_EDIT = "mpdx : contacts : detail : edit"
const val SCREEN_CONTACTS_EDIT_COMMITMENT = "mpdx : contacts : detail : edit : commitment"
const val SCREEN_CONTACTS_EDIT_ADDRESS = "mpdx : contacts : detail : edit : address"
const val SCREEN_CONTACTS_DONATIONS = "mpdx : contacts : detail : donations"
const val SCREEN_CONTACTS_DONATIONS_HISTORY = "mpdx : contacts : detail : donations : history"
const val SCREEN_CONTACTS_FILTER = "mpdx : contacts : filter contacts"
const val SCREEN_CONTACTS_FILTER_CHURCH = "mpdx : contacts : filter contacts : church"
const val SCREEN_CONTACTS_FILTER_CITY = "mpdx : contacts : filter contacts : city"
const val SCREEN_CONTACTS_FILTER_LIKELY = "mpdx : contacts : filter contacts : likely to give"
const val SCREEN_CONTACTS_FILTER_REFERRER = "mpdx : contacts : filter contacts : referrer"
const val SCREEN_CONTACTS_FILTER_STATUS = "mpdx : contacts : filter contacts : status"
const val SCREEN_CONTACTS_FILTER_STATE = "mpdx : contacts : filter contacts : state"
const val SCREEN_CONTACTS_FILTER_TAGS = "mpdx : contacts : filter contacts : tags"
const val SCREEN_CONTACTS_FILTER_TIMEZONE = "mpdx : contacts : filter contacts : timezone"
const val SCREEN_CONTACTS_INFO = "mpdx : contacts : detail : info"
const val SCREEN_CONTACTS_NOTES = "mpdx : contacts : detail : notes"
const val SCREEN_CONTACTS_TASKS = "mpdx : contacts : detail : tasks"

const val SCREEN_DASHBOARD_VIEW = "dashboard"
const val SCREEN_DASHBOARD_CONNECT = "mpdx : dashboard : connect"
const val SCREEN_DASHBOARD_COMMITMENT_CARE = "mpdx : dashboard : care"

const val SCREEN_DONATIONS_MONTH = "mpdx : donations : month"
const val SCREEN_DONATIONS_VIEW_EDIT = "mpdx : donations : view/edit"
const val SCREEN_DONATIONS_VIEW_EDIT_CURRENCY = "mpdx : donations : view/edit : currency"
const val SCREEN_DONATIONS_VIEW_EDIT_APPEALS = "mpdx : donations : view/edit : appeals"
const val SCREEN_DONATIONS_VIEW_EDIT_DESIGNATION_ACCOUNT = "mpdx : donations : view/edit : designation account"
const val SCREEN_DONATIONS_VIEW_EDIT_PARTNER_ACCOUNTS = "mpdx : donations : view/edit : partner account"
const val SCREEN_DONATIONS_YEAR = "mpdx : donations : year"

const val SCREEN_NOTIFICATION_ALL = "mpdx : notifications : all"
const val SCREEN_NOTIFICATION_FILTER = "mpdx : notifications : filter notifications"
const val SCREEN_NOTIFICATION_UNREAD = "mpdx : notifications : unread"

const val SCREEN_ON_BOARDING_DOWNLOADING = "mpdx : onboarding : download"
const val SCREEN_ON_BOARDING_SIGN_IN = "mpdx : onboarding : signin"

const val SCREEN_SETTINGS = "mpdx : settings"
const val SCREEN_SETTINGS_ACCOUNT_LIST = "mpdx : settings : account list"
const val SCREEN_SETTINGS_NOTIFICATION_PREFERENCES = "mpdx : settings : notification preferences"
const val SCREEN_SETTINGS_PRIVACY_POLICY = "mpdx : settings : privacy policy"

const val SCREEN_TASKS_CURRENT = "mpdx : tasks : current tasks"
const val SCREEN_TASK_DETAIL = "mpdx : tasks : detail"
const val SCREEN_TASKS_FILTER_CONTACT_CITY = "mpdx : filter tasks : contact city"
const val SCREEN_TASKS_FILTER_CONTACT_CHURCH = "mpdx : filter tasks : contact church"
const val SCREEN_TASKS_FILTER_CONTACT_LIKELY_GIVE = "mpdx : filter tasks : contact likely to give"
const val SCREEN_TASKS_FILTER_CONTACT_REFERRER = "mpdx : filter tasks : contact referrer"
const val SCREEN_TASKS_FILTER_CONTACT_STATE = "mpdx : filter tasks : contact state"
const val SCREEN_TASKS_FILTER = "mpdx : tasks : filter tasks"
const val SCREEN_TASKS_FILTER_CONTACT_TIMEZONE = "mpdx : filter tasks : contact timezone"
const val SCREEN_TASKS_FILTER_TAGS = "mpdx : filter tasks : tags"
const val SCREEN_TASKS_FILTER_ACTION_TYPE = "mpdx : tasks : filter tasks : action type"
const val SCREEN_TASKS_FILTER_CONTACT_STATUS = "mpdx : tasks : filter tasks : contact status"
const val SCREEN_TASKS_HISTORY = "mpdx : tasks : task history"
const val SCREEN_TASKS_EDITOR_NEW = "mpdx : tasks : new task"
const val SCREEN_TASKS_EDITOR_LOG = "mpdx : tasks : log task"
const val SCREEN_TASKS_EDITOR_EDIT = "mpdx : tasks : edit task"
const val SCREEN_TASKS_EDITOR_FINISH = "mpdx : tasks : finish task"

private const val SITE_SECTION_APPEALS = "Appeals"
private const val SITE_SECTION_COACHING = "Coaching"
private const val SITE_SECTION_CONTACT = "Contacts"
private const val SITE_SECTION_DASHBOARD = "Dashboard"
private const val SITE_SECTION_DONATIONS = "Donations"
private const val SITE_SECTION_NOTIFICATION = "Notifications"
private const val SITE_SECTION_ON_BOARDING = "Onboarding"
private const val SITE_SECTION_SETTINGS = "Settings"
private const val SITE_SECTION_TASKS = "Tasks"

class AnalyticsScreenEvent(val screen: String) : AnalyticsBaseEvent() {
    override val siteSection
        get() = when (screen) {
            SCREEN_APPEALS_ASKED,
            SCREEN_APPEALS_COMMITTED,
            SCREEN_APPEALS_GIVEN,
            SCREEN_APPEALS_LIST,
            SCREEN_APPEALS_RECEIVED -> SITE_SECTION_APPEALS
            SCREEN_COACHING_DETAIL_MONTH,
            SCREEN_COACHING_DETAIL_WEEK,
            SCREEN_COACHING_LIST -> SITE_SECTION_COACHING
            SCREEN_CONTACTS,
            SCREEN_CONTACTS_EDIT,
            SCREEN_CONTACTS_EDIT_COMMITMENT,
            SCREEN_CONTACTS_EDIT_ADDRESS,
            SCREEN_CONTACTS_DONATIONS,
            SCREEN_CONTACTS_DONATIONS_HISTORY,
            SCREEN_CONTACTS_FILTER,
            SCREEN_CONTACTS_FILTER_CHURCH,
            SCREEN_CONTACTS_FILTER_CITY,
            SCREEN_CONTACTS_FILTER_LIKELY,
            SCREEN_CONTACTS_FILTER_REFERRER,
            SCREEN_CONTACTS_FILTER_STATUS,
            SCREEN_CONTACTS_FILTER_STATE,
            SCREEN_CONTACTS_FILTER_TAGS,
            SCREEN_CONTACTS_FILTER_TIMEZONE,
            SCREEN_CONTACTS_INFO,
            SCREEN_CONTACTS_NOTES,
            SCREEN_CONTACTS_TASKS -> SITE_SECTION_CONTACT
            SCREEN_DASHBOARD_VIEW,
            SCREEN_DASHBOARD_COMMITMENT_CARE -> SITE_SECTION_DASHBOARD
            SCREEN_DONATIONS_MONTH,
            SCREEN_DONATIONS_VIEW_EDIT,
            SCREEN_DONATIONS_VIEW_EDIT_DESIGNATION_ACCOUNT,
            SCREEN_DONATIONS_VIEW_EDIT_PARTNER_ACCOUNTS,
            SCREEN_DONATIONS_YEAR -> SITE_SECTION_DONATIONS
            SCREEN_NOTIFICATION_UNREAD,
            SCREEN_NOTIFICATION_ALL,
            SCREEN_NOTIFICATION_FILTER -> SITE_SECTION_NOTIFICATION
            SCREEN_ON_BOARDING_DOWNLOADING,
            SCREEN_ON_BOARDING_SIGN_IN -> SITE_SECTION_ON_BOARDING
            SCREEN_SETTINGS,
            SCREEN_SETTINGS_ACCOUNT_LIST,
            SCREEN_SETTINGS_NOTIFICATION_PREFERENCES,
            SCREEN_SETTINGS_PRIVACY_POLICY -> SITE_SECTION_SETTINGS
            SCREEN_TASKS_CURRENT,
            SCREEN_TASKS_FILTER,
            SCREEN_TASKS_FILTER_ACTION_TYPE,
            SCREEN_TASKS_FILTER_CONTACT_CHURCH,
            SCREEN_TASKS_FILTER_CONTACT_CITY,
            SCREEN_TASKS_FILTER_CONTACT_LIKELY_GIVE,
            SCREEN_TASKS_FILTER_CONTACT_REFERRER,
            SCREEN_TASKS_FILTER_CONTACT_STATE,
            SCREEN_TASKS_FILTER_CONTACT_STATUS,
            SCREEN_TASKS_FILTER_CONTACT_TIMEZONE,
            SCREEN_TASKS_FILTER_TAGS,
            SCREEN_TASKS_HISTORY,
            SCREEN_TASKS_EDITOR_LOG,
            SCREEN_TASKS_EDITOR_EDIT,
            SCREEN_TASKS_EDITOR_FINISH -> SITE_SECTION_TASKS
            else -> null
        }

    override val siteSubSection: String?
        get() {
            when (screen) {
// TODO: Extract SiteSection
            }
            return null
        }
}
