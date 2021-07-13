package org.mpdx.android.features.tasks.model

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.annotations.Ignore
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import java.util.Date
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPlaceholder
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPostCreate
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.R
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_CREATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_IN_DB_AT
import org.mpdx.android.base.model.ChangeAwareItem
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.TagsConcern
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.base.model.addUnique
import org.mpdx.android.base.model.sanitizeTag
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.base.testing.OpenForTesting
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.core.model.User
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.realm.forTask
import org.mpdx.android.features.contacts.realm.getContacts
import org.mpdx.android.features.contacts.realm.getEmailAddresses
import org.mpdx.android.features.contacts.realm.getPhoneNumbers
import org.mpdx.android.utils.StringResolver
import org.mpdx.android.utils.toDate
import org.mpdx.android.utils.toRealmList
import org.mpdx.android.utils.toZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

private const val JSON_API_TYPE_TASK = "tasks"

private const val JSON_ACCOUNT_LIST = "account_list"
private const val JSON_COMPLETED_AT = "completed_at"
private const val JSON_NEXT_ACTION = "next_action"
private const val JSON_NOTIFICATION_TYPE = "notification_type"
private const val JSON_NOTIFICATION_TIME = "notification_time_before"
private const val JSON_NOTIFICATION_TIME_UNIT = "notification_time_unit"
private const val JSON_PHONE_NUMBERS = "phone_numbers"
private const val JSON_RESULT = "result"
private const val JSON_START_AT = "start_at"
private const val JSON_TAGS = "tag_list"
private const val JSON_TYPE = "activity_type"

private const val JSON_VALUE_NOTIFICATION_TYPE_EMAIL = "email"
private const val JSON_VALUE_NOTIFICATION_TYPE_MOBILE = "mobile"
private const val JSON_VALUE_NOTIFICATION_TYPE_BOTH = "both"

private const val JSON_NOTIFICATION_TIME_UNIT_MINUTES = "minutes"
private const val JSON_NOTIFICATION_TIME_UNIT_HOURS = "hours"

private const val JSON_RESULT_ATTEMPTED = "Attempted"
private const val JSON_RESULT_ATTEMPTED_LEFT_MESSAGE = "Attempted - Left Message"
private const val JSON_RESULT_COMPLETED = "Completed"
private const val JSON_RESULT_DONE = "Done"
private const val JSON_RESULT_RECEIVED = "Received"

@JsonApiType(JSON_API_TYPE_TASK)
open class Task : RealmObject(), UniqueItem, ChangeAwareItem, JsonApiModel, TagsConcern {
    companion object {
        const val JSON_COMMENTS = "comments"
        const val JSON_COMPLETED = "completed"
        const val JSON_CONTACTS = "contacts"
        const val JSON_USER = "user"
        internal const val JSON_SUBJECT = "subject"
        internal const val JSON_SUBJECT_HIDDEN = "subject_hidden"
        const val JSON_TASK_CONTACTS = "activity_contacts"

        const val JSON_FILTER_CONTACT_IDS = "contact_ids"

        const val ACTIVITY_TYPE_APPOINTMENT = "Appointment"
        private const val RESULT_APPOINTMENT_SCHEDULED = "Appointment Scheduled"
        @JvmStatic
        val RESULTS_CALL = arrayOf(
            JSON_RESULT_ATTEMPTED_LEFT_MESSAGE,
            JSON_RESULT_ATTEMPTED,
            JSON_RESULT_COMPLETED,
            RESULT_APPOINTMENT_SCHEDULED
        )
        val RESULTS_CALL_RES = intArrayOf(
            R.string.result_attempted_left_message,
            R.string.result_attempted,
            R.string.result_completed,
            R.string.result_appointment_scheduled
        )
    }

    @PrimaryKey
    @JsonApiId
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType get() = JSON_API_TYPE_TASK

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false
    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute(JSON_TYPE)
    var activityType: String? = null
        set(value) {
            if (value != field) markChanged(JSON_TYPE)
            field = value
        }

    @JsonApiAttribute(JSON_COMPLETED)
    var isCompleted: Boolean = false
        set(value) {
            if (value != field) markChanged(JSON_COMPLETED)
            field = value
        }

    @JsonApiAttribute(JSON_COMPLETED_AT)
    private var completedAtValue: Date? = null
        set(value) {
            if (value != field) markChanged(JSON_COMPLETED_AT)
            field = value
        }

    @JsonApiAttribute(JSON_NOTIFICATION_TYPE)
    private var notificationTypeValue: String? = null
        set(value) {
            if (value.orEmpty() != field.orEmpty()) markChanged(JSON_NOTIFICATION_TYPE)
            field = value
        }

    @JsonApiAttribute(JSON_NOTIFICATION_TIME)
    var notificationTimeBefore: Int = 0
        set(value) {
            if (value != field) markChanged(JSON_NOTIFICATION_TIME)
            field = value.coerceAtLeast(0)
        }

    @JsonApiAttribute(JSON_NOTIFICATION_TIME_UNIT)
    private var notificationTimeUnitValue: String? = null
        set(value) {
            if (value.orEmpty() != field.orEmpty()) markChanged(JSON_NOTIFICATION_TIME_UNIT)
            field = value
        }

    @JsonApiAttribute(JSON_RESULT)
    private var resultValue: String? = null
        set(value) {
            if (value != field) markChanged(JSON_RESULT)
            field = value
        }

    @JsonApiAttribute(JSON_NEXT_ACTION)
    var nextAction: String? = null
        set(value) {
            if (value != field) markChanged(JSON_NEXT_ACTION)
            field = value
        }

    @JsonApiAttribute(JSON_START_AT)
    var startAt: Date? = null
        set(value) {
            if (value != field) markChanged(JSON_START_AT)
            field = value
        }

    @JsonApiAttribute(JSON_SUBJECT)
    var subject: String? = null
        set(value) {
            if (value.orEmpty() != field.orEmpty()) markChanged(JSON_SUBJECT)
            if (trackingChanges && value.isNullOrBlank()) clearChanged(JSON_SUBJECT)
            field = value
            isSubjectHidden = value.isNullOrBlank()
        }

    @JsonApiAttribute(JSON_SUBJECT_HIDDEN)
    var isSubjectHidden: Boolean = false
        get() = field || subject.isNullOrBlank()
        private set(value) {
            if (value != field) markChanged(JSON_SUBJECT_HIDDEN)
            field = value
            if (value && !subject.isNullOrBlank()) subject = null
        }

    @JsonApiAttribute(JSON_TAGS, serialize = false)
    override var tags: RealmList<String>? = null
        get() = field ?: RealmList<String>().also { if (!isManaged || realm!!.isInTransaction) field = it }
        set(value) {
            val sanitized = value?.mapNotNull { sanitizeTag(it) }?.toSet()
            if (field?.toSet().orEmpty() != sanitized.orEmpty()) markChanged(JSON_TAGS)
            field = sanitized?.toRealmList()
        }
    // HACK: this variable is used to push tags to the API. This should no longer be necessary after MPDX-6068
    @Ignore
    @VisibleForTesting
    @JsonApiAttribute(JSON_TAGS, deserialize = false)
    internal var hackTags: String? = null
        private set

    @JsonApiAttribute("comments_count")
    var commentsCount: Int = 0

    @JsonApiAttribute(JSON_ATTR_CREATED_AT)
    private var createdAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_AT)
    var updatedAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDatabaseAt: Date? = null

    // region Synthetic Attributes
    var dueDate
        get() = startAt?.toZonedDateTime()
        set(value) {
            startAt = value?.toDate()
        }

    var completedAt
        get() = completedAtValue?.toZonedDateTime()
        set(value) {
            completedAtValue = value?.toDate()
        }

    var result
        get() = Result.fromValue(resultValue)
        set(value) {
            resultValue = value?.value
        }

    var notificationType: NotificationType
        get() = NotificationType.fromApiValue(notificationTypeValue)
        set(value) {
            notificationTypeValue = value.apiValue
        }

    var notificationTimeUnit: ChronoUnit?
        get() = when (notificationTimeUnitValue) {
            JSON_NOTIFICATION_TIME_UNIT_MINUTES -> ChronoUnit.MINUTES
            JSON_NOTIFICATION_TIME_UNIT_HOURS -> ChronoUnit.HOURS
            else -> null
        }
        set(value) {
            notificationTimeUnitValue = when (value) {
                ChronoUnit.MINUTES -> JSON_NOTIFICATION_TIME_UNIT_MINUTES
                ChronoUnit.HOURS -> JSON_NOTIFICATION_TIME_UNIT_HOURS
                else -> null
            }
        }
    // endregion Synthetic Attributes
    // endregion Attributes

    // region Relationships
    @JsonApiAttribute(JSON_ACCOUNT_LIST)
    var accountList: AccountList? = null

    @Ignore
    @JsonApiAttribute(JSON_TASK_CONTACTS)
    internal var apiTaskContacts: List<TaskContact>? = null
    @JsonApiIgnore
    @OpenForTesting
    @LinkingObjects("task")
    internal open val taskContacts: RealmResults<TaskContact>? = null

    fun getContacts(includeDeleted: Boolean = false) = realm?.getContacts(includeDeleted)?.forTask(id)
    val firstContact get() = getContacts()?.findFirst()

    fun getEmailAddresses(includeDeleted: Boolean = false) = realm?.getEmailAddresses(includeDeleted)?.forTask(id)
    fun getPhoneNumbers(includeDeleted: Boolean = false) = realm?.getPhoneNumbers(includeDeleted)?.forTask(id)

    @Ignore
    @JsonApiAttribute(JSON_COMMENTS, serialize = false)
    internal var apiComments: List<Comment>? = null
    @JsonApiIgnore
    @LinkingObjects("task")
    private val comments: RealmResults<Comment>? = null

    // TODO: remove JvmOverloads when this is no longer called from Java
    @JvmOverloads
    fun getComments(includeDeleted: Boolean = false): RealmQuery<Comment>? = comments?.where()
        ?.includeDeleted(includeDeleted)

    @Ignore
    @JsonApiAttribute(JSON_CONTACTS)
    private var contacts: List<Contact>? = null
    @JsonApiIgnore
    var contactName: String? = null
        get() = field ?: contacts?.firstOrNull()?.envelopeGreeting

    @JsonApiAttribute(JSON_USER)
    var user: User? = null
        set(value) {
            if (field?.id != value?.id) markChanged(JSON_USER)
            field = value
        }
    // endregion Relationships

    // region ChangeAwareItem
    @JsonApiIgnore
    override var isNew = false
    @JsonApiIgnore
    override var isDeleted = false
    @Ignore
    @JsonApiIgnore
    override var trackingChanges = false
    @JsonApiIgnore
    override var changedFieldsStr: String = ""

    override fun mergeChangedField(source: ChangeAwareItem, field: String) {
        if (source is Task) {
            when (field) {
                // attributes
                JSON_COMPLETED -> isCompleted = source.isCompleted
                JSON_COMPLETED_AT -> completedAtValue = source.completedAtValue
                JSON_NEXT_ACTION -> nextAction = source.nextAction
                JSON_NOTIFICATION_TYPE -> notificationTypeValue = source.notificationTypeValue
                JSON_NOTIFICATION_TIME -> notificationTimeBefore = source.notificationTimeBefore
                JSON_NOTIFICATION_TIME_UNIT -> notificationTimeUnitValue = source.notificationTimeUnitValue
                JSON_RESULT -> resultValue = source.resultValue
                JSON_START_AT -> startAt = source.startAt
                JSON_SUBJECT -> subject = source.subject
                JSON_SUBJECT_HIDDEN -> isSubjectHidden = source.isSubjectHidden
                JSON_TAGS -> tags = source.tags
                JSON_TYPE -> activityType = source.activityType
                JSON_USER -> user = source.user
            }
        }
    }
    // endregion ChangeAwareItem

    // region TagsConcern
    override fun onTagsChanged() {
        markChanged(JSON_TAGS)
    }
    // endregion TagsConcern

    private var notificationShown: Boolean = false

    // region API logic
    @JsonApiPostCreate
    private fun clearHiddenSubjects() {
        if (isSubjectHidden) subject = null
    }

    @JsonApiPostCreate
    private fun populateCommentTaskReference() {
        apiComments?.forEach { it.task = this }
    }

    @JsonApiPostCreate
    private fun flattenContacts() {
        contacts?.let {
            contactName = it.firstOrNull()?.envelopeGreeting
        }
    }

    @JsonApiPostCreate
    private fun populateAssignedUserAccountListReference() {
        accountList?.let { user?.accountLists?.addUnique(it) }
    }

    override fun prepareForApi() {
        sortContactsForApi()
        hackTagListForApi()
    }

    private fun sortContactsForApi() {
        // new contacts need to be sent in the contacts relationship
        contacts = apiTaskContacts?.filter { it.isNew && !it.isDeleted }?.mapNotNull { it.contact }
        // deleted contacts need to be sent in the taskContacts relationship
        apiTaskContacts = apiTaskContacts?.filter { it.isDeleted }
    }

    private fun hackTagListForApi() {
        // HACK: the API returns the tag_list as an array, but only accepts it as a comma separated string
        hackTags = tagsAsString
    }
    // endregion API logic

    val tagsAsString get() = tags?.joinToString(", ") ?: ""

    fun setTags(csv: String?) {
        tags = csv?.split(",")?.map { it.trim() }?.filterNot { it.isEmpty() }?.toRealmList()
    }

    val notificationTimeBeforeSeconds: Int
        get() = if (notificationTimeBefore == 0 || notificationTimeUnit == null) {
            -1
        } else notificationTimeBefore * if (notificationTimeUnit == ChronoUnit.HOURS) 3600 else 60

    fun hasMobileNotifications() = when (notificationType) {
        NotificationType.MOBILE, NotificationType.BOTH ->
            startAt != null && notificationTimeBefore > 0 && notificationTimeUnit != null
        else -> false
    }

    fun getDescription(stringResolver: StringResolver): String? {
        return if (isSubjectHidden) {
            stringResolver.getString(R.string.no_subject_description, activityType, getContactNameText(stringResolver))
        } else {
            subject
        }
    }

    fun getContactNameText(stringResolver: StringResolver) = when (1) {
        0 -> stringResolver.getString(R.string.no_contacts)
        1 -> contactName ?: stringResolver.getString(R.string.contact_name_unavailable)
        else -> stringResolver.getString(R.string.multiple_contacts)
    }

    enum class Result(val value: String, @StringRes val label: Int) {
        ATTEMPTED_LEFT_MESSAGE(JSON_RESULT_ATTEMPTED_LEFT_MESSAGE, R.string.result_attempted_left_message),
        ATTEMPTED(JSON_RESULT_ATTEMPTED, R.string.result_attempted),
        COMPLETED(JSON_RESULT_COMPLETED, R.string.result_completed),
        RECEIVED(JSON_RESULT_RECEIVED, R.string.task_result_received),
        DONE(JSON_RESULT_DONE, R.string.task_result_done);

        companion object {
            @JvmStatic
            fun fromValue(value: String?) = value?.let { values().firstOrNull { it.value == value } }
        }
    }

    enum class NotificationType(@StringRes val labelRes: Int, val apiValue: String?) {
        NONE(R.string.alert_mode_none, null),
        EMAIL(R.string.alert_mode_email, JSON_VALUE_NOTIFICATION_TYPE_EMAIL),
        MOBILE(R.string.alert_mode_mobile, JSON_VALUE_NOTIFICATION_TYPE_MOBILE),
        BOTH(R.string.alert_mode_both, JSON_VALUE_NOTIFICATION_TYPE_BOTH);

        companion object {
            @JvmField
            val DEFAULT = NONE

            @JvmStatic
            fun fromApiValue(value: String?) = values().firstOrNull { it.apiValue == value } ?: DEFAULT
        }
    }
}
