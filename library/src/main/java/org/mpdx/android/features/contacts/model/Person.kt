package org.mpdx.android.features.contacts.model

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
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
import org.mpdx.android.base.model.ChangeAwareItem
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.LocalAttributes
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.utils.dayOfYear
import org.mpdx.android.utils.localDateOrNull
import org.mpdx.android.utils.monthDayComparator
import org.mpdx.android.utils.monthDayOrNull
import org.threeten.bp.MonthDay

private const val JSON_API_TYPE_PERSON = "people"

private const val JSON_FIRST_NAME = "first_name"
private const val JSON_MIDDLE_NAME = "middle_name"
private const val JSON_LAST_NAME = "last_name"
private const val JSON_ANNIVERSARY_YEAR = "anniversary_year"
private const val JSON_ANNIVERSARY_MONTH = "anniversary_month"
private const val JSON_ANNIVERSARY_DAY = "anniversary_day"
private const val JSON_BIRTHDAY_YEAR = "birthday_year"
private const val JSON_BIRTHDAY_MONTH = "birthday_month"
private const val JSON_BIRTHDAY_DAY = "birthday_day"
private const val JSON_EMPLOYER = "employer"
private const val JSON_SUFFIX = "suffix"
private const val JSON_TITLE = "title"

@JsonApiType(JSON_API_TYPE_PERSON)
open class Person : RealmObject(), UniqueItem, JsonApiModel, ChangeAwareItem, LocalAttributes {
    companion object {
        const val JSON_EMAIL_ADDRESSES = "email_addresses"
        const val JSON_FACEBOOK_ACCOUNT = "facebook_accounts"
        const val JSON_LINKEDIN_ACCOUNT = "linkedin_accounts"
        const val JSON_PHONE_NUMBERS = "phone_numbers"
        const val JSON_TWITTER = "twitter_accounts"
        const val JSON_WEBSITE = "websites"

        @JvmStatic
        fun birthdayComparator(startAt: MonthDay = MonthDay.of(1, 1)): Comparator<Person?> =
            compareBy(monthDayComparator(startAt)) { it?.birthday }
    }

    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType get() = JSON_API_TYPE_PERSON

    @JsonApiPlaceholder
    override var isPlaceholder = false
    @Ignore
    @JsonApiIgnore
    override var replacePlaceholder = false
    // endregion JsonApiModel

    // region Attributes
    @SerializedName(JSON_FIRST_NAME)
    @JsonApiAttribute(JSON_FIRST_NAME)
    var firstName: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_FIRST_NAME)
            field = value
        }

    @SerializedName(JSON_MIDDLE_NAME)
    @JsonApiAttribute(JSON_MIDDLE_NAME)
    var middleName: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_MIDDLE_NAME)
            field = value
        }

    @SerializedName(JSON_LAST_NAME)
    @JsonApiAttribute(JSON_LAST_NAME)
    var lastName: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_LAST_NAME)
            field = value
        }

    @JsonApiAttribute("avatar")
    @SerializedName("avatar")
    var avatarUrl: String? = null

    @JsonApiAttribute(JSON_BIRTHDAY_YEAR)
    @SerializedName(JSON_BIRTHDAY_YEAR)
    private var birthdayYear: Int? = null
        set(value) {
            if (field != value) markChanged(JSON_BIRTHDAY_YEAR)
            field = value
        }
    @JsonApiAttribute(JSON_BIRTHDAY_MONTH)
    @SerializedName(JSON_BIRTHDAY_MONTH)
    private var birthdayMonth: Int? = null
        set(value) {
            if (field != value) markChanged(JSON_BIRTHDAY_MONTH)
            field = value
            birthdayDayOfYear
        }
    @JsonApiAttribute(JSON_BIRTHDAY_DAY)
    @SerializedName(JSON_BIRTHDAY_DAY)
    private var birthdayDay: Int? = null
        set(value) {
            if (field != value) markChanged(JSON_BIRTHDAY_DAY)
            field = value
            birthdayDayOfYear
        }

    @JsonApiAttribute(JSON_ANNIVERSARY_YEAR)
    @SerializedName(JSON_ANNIVERSARY_YEAR)
    private var anniversaryYear: Int? = null
        set(value) {
            if (field != value) markChanged(JSON_ANNIVERSARY_YEAR)
            field = value
        }
    @JsonApiAttribute(JSON_ANNIVERSARY_MONTH)
    @SerializedName(JSON_ANNIVERSARY_MONTH)
    private var anniversaryMonth: Int? = null
        set(value) {
            if (field != value) markChanged(JSON_ANNIVERSARY_MONTH)
            field = value
            anniversaryDayOfYear
        }
    @JsonApiAttribute(JSON_ANNIVERSARY_DAY)
    @SerializedName(JSON_ANNIVERSARY_DAY)
    private var anniversaryDay: Int? = null
        set(value) {
            if (field != value) markChanged(JSON_ANNIVERSARY_DAY)
            field = value
            anniversaryDayOfYear
        }

    @JsonApiAttribute(JSON_TITLE)
    @SerializedName(JSON_TITLE)
    var title: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_TITLE)
            field = value
        }

    @JsonApiAttribute(JSON_SUFFIX)
    @SerializedName(JSON_SUFFIX)
    var suffix: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_SUFFIX)
            field = value
        }

    @JsonApiAttribute(JSON_EMPLOYER)
    @SerializedName(JSON_EMPLOYER)
    var employer: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_EMPLOYER)
            field = value
        }
    // endregion Attributes

    // region Relationships

    @Ignore
    @JsonApiAttribute(JSON_PHONE_NUMBERS, serialize = false)
    internal var apiPhoneNumbers: List<PhoneNumber>? = null
    @JsonApiIgnore
    @LinkingObjects("person")
    private val phoneNumbers: RealmResults<PhoneNumber>? = null

    fun getPhoneNumbers(includeDeleted: Boolean = false): RealmQuery<PhoneNumber>? = phoneNumbers?.where()
        ?.includeDeleted(includeDeleted)
        ?.sort(PhoneNumberFields.CREATED_AT, Sort.ASCENDING, PhoneNumberFields.ID, Sort.ASCENDING)

    @Ignore
    @JsonApiAttribute(JSON_EMAIL_ADDRESSES, serialize = false)
    internal var apiEmailAddresses: List<EmailAddress>? = null
    @JsonApiIgnore
    @LinkingObjects("person")
    private val emailAddresses: RealmResults<EmailAddress>? = null

    fun getEmailAddresses(includeDeleted: Boolean = false): RealmQuery<EmailAddress>? = emailAddresses?.where()
        ?.includeDeleted(includeDeleted)

    @Ignore
    @JsonApiAttribute(JSON_FACEBOOK_ACCOUNT, serialize = false)
    internal var apiFacebookAccounts: List<FacebookAccount>? = null
    @JsonApiIgnore
    @LinkingObjects("person")
    private val facebookAccounts: RealmResults<FacebookAccount>? = null

    fun getFacebookAccounts(includeDeleted: Boolean = false): RealmQuery<FacebookAccount>? = facebookAccounts?.where()
        ?.includeDeleted(includeDeleted)

    @Ignore
    @JsonApiAttribute(JSON_LINKEDIN_ACCOUNT, serialize = false)
    internal var apiLinkedInAccounts: List<LinkedInAccount>? = null
    @JsonApiIgnore
    @LinkingObjects("person")
    private val linkedInAccounts: RealmResults<LinkedInAccount>? = null

    fun getLinkedInAccounts(includeDeleted: Boolean = false): RealmQuery<LinkedInAccount>? = linkedInAccounts?.where()
        ?.includeDeleted(includeDeleted)

    @Ignore
    @JsonApiAttribute(JSON_TWITTER, serialize = false)
    internal var apiTwitterAccounts: List<TwitterAccount>? = null
    @JsonApiIgnore
    @LinkingObjects("person")
    private val twitterAccounts: RealmResults<TwitterAccount>? = null

    fun getTwitterAccounts(includeDeleted: Boolean = false): RealmQuery<TwitterAccount>? = twitterAccounts?.where()
        ?.includeDeleted(includeDeleted)

    @Ignore
    @JsonApiAttribute(JSON_WEBSITE, serialize = false)
    internal var apiWebsites: List<Website>? = null
    @JsonApiIgnore
    @LinkingObjects("person")
    private val websites: RealmResults<Website>? = null

    fun getWebsites(includeDeleted: Boolean = false): RealmQuery<Website>? = websites?.where()
        ?.includeDeleted(includeDeleted)

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
    override var changedFieldsStr = ""

    override fun mergeChangedField(source: ChangeAwareItem, field: String) {
        if (source is Person) {
            when (field) {
                // attributes
                JSON_FIRST_NAME -> firstName = source.firstName
                JSON_MIDDLE_NAME -> middleName = source.middleName
                JSON_LAST_NAME -> lastName = source.lastName
                JSON_ANNIVERSARY_YEAR -> anniversaryYear = source.anniversaryYear
                JSON_ANNIVERSARY_MONTH -> anniversaryMonth = source.anniversaryMonth
                JSON_ANNIVERSARY_DAY -> anniversaryDay = source.anniversaryDay
                JSON_BIRTHDAY_YEAR -> birthdayYear = source.birthdayYear
                JSON_BIRTHDAY_MONTH -> birthdayMonth = source.birthdayMonth
                JSON_BIRTHDAY_DAY -> birthdayDay = source.birthdayDay
                JSON_EMPLOYER -> employer = source.employer
                JSON_SUFFIX -> suffix = source.suffix
                JSON_TITLE -> title = source.title
            }
        }
    }

    // endregion ChangeAwareItem

    // region DBItem

    @JsonApiAttribute("created_at")
    @SerializedName("created_at")
    var createdAt: Date? = null
    @JsonApiAttribute("updated_at")
    @SerializedName("updated_at")
    private var updatedAt: Date? = null
    @JsonApiAttribute("updated_in_db_at")
    @SerializedName("updated_in_db_at")
    var updatedInDatabaseAt: Date? = null

    // endregion DBItem

    // region Local Attributes
    @JsonApiIgnore
    var contact: Contact? = null
    @Deprecated("You should use the contact field directly to retrieve the contact id", ReplaceWith("contact?.id"))
    val contactId: String?
        get() = contact?.id

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is Person) {
            contact = contact ?: existing.contact
        }
    }
    // endregion Local Attributes

    // region Generated Attributes

    var birthDate
        get() = localDateOrNull(birthdayYear, birthdayMonth, birthdayDay)
        set(value) {
            birthdayYear = value?.year
            birthdayMonth = value?.monthValue
            birthdayDay = value?.dayOfMonth
        }
    var birthday
        get() = monthDayOrNull(birthdayMonth, birthdayDay)
        set(value) {
            birthdayYear = null
            birthdayMonth = value?.monthValue
            birthdayDay = value?.dayOfMonth
        }

    @JsonApiIgnore
    private var birthdayDayOfYear: Int? = null
        get() = birthday?.dayOfYear.also { field = it }
        private set(_) {
            field = birthday?.dayOfYear
        }

    var anniversaryDate
        get() = localDateOrNull(anniversaryYear, anniversaryMonth, anniversaryDay)
        set(value) {
            anniversaryYear = value?.year
            anniversaryMonth = value?.monthValue
            anniversaryDay = value?.dayOfMonth
        }
    var anniversary
        get() = monthDayOrNull(anniversaryMonth, anniversaryDay)
        set(value) {
            anniversaryYear = null
            anniversaryMonth = value?.monthValue
            anniversaryDay = value?.dayOfMonth
        }
    @JsonApiIgnore
    private var anniversaryDayOfYear: Int? = null
        get() = anniversary?.dayOfYear.also { field = it }
        private set(_) {
            field = anniversary?.dayOfYear
        }

    // endregion Generated Attributes

    // region API logic

    @JsonApiPostCreate
    private fun populateEmailAddressPersonRelationship() = apiEmailAddresses?.forEach { it.person = this }

    @JsonApiPostCreate
    private fun populateFacebookAccountsRelationship() = apiFacebookAccounts?.forEach { it.person = this }

    @JsonApiPostCreate
    private fun populatePhoneNumberPersonRelationship() = apiPhoneNumbers?.forEach { it.person = this }

    @JsonApiPostCreate
    private fun populateLinkedinAccountsRelationship() = apiLinkedInAccounts?.forEach { it.person = this }

    @JsonApiPostCreate
    private fun populateTwitterAccountsRelationship() = apiTwitterAccounts?.forEach { it.person = this }

    @JsonApiPostCreate
    private fun populateWebsitesRelationship() = apiWebsites?.forEach { it.person = this }

    @JsonApiPostCreate
    private fun calculateBirthdayDayOfYear() = birthdayDayOfYear

    @JsonApiPostCreate
    private fun calculateAnniversaryDayOfYear() = anniversaryDayOfYear

    // endregion API logic
}
