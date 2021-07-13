package org.mpdx.android.features.contacts.model

import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Ignore
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import io.realm.kotlin.oneOf
import io.realm.kotlin.where
import java.util.Date
import kotlin.math.roundToInt
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPlaceholder
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPostCreate
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.R
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_IN_DB_AT
import org.mpdx.android.base.model.ChangeAwareItem
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.TagsConcern
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.base.model.sanitizeTag
import org.mpdx.android.base.realm.PACKED_FIELD_SEPARATOR
import org.mpdx.android.core.data.typeadapter.StringMapWrapper
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.features.appeals.model.AskedContact
import org.mpdx.android.features.contacts.realm.forContact
import org.mpdx.android.features.contacts.realm.getAddresses
import org.mpdx.android.features.contacts.realm.getPeople
import org.mpdx.android.features.donations.model.Donation
import org.mpdx.android.features.donations.model.DonationFields
import org.mpdx.android.features.donations.realm.sortByDate
import org.mpdx.android.features.tasks.model.TaskContact
import org.mpdx.android.utils.StringResolver
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.toDate
import org.mpdx.android.utils.toLocalDate
import org.mpdx.android.utils.toRealmList
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoUnit

const val JSON_API_TYPE_CONTACT = "contacts"

private const val JSON_REFERRED_BY = "contacts_that_referred_me"
private const val JSON_ACCOUNT_LIST = "account_list"
private const val JSON_ENVELOPE_GREETING = "envelope_greeting"
private const val JSON_GREETING = "greeting"
private const val JSON_LAST_SIX_DONATIONS = "last_six_donations"
private const val JSON_LIKELY_TO_GIVE = "likely_to_give"
private const val JSON_LOCALE = "locale"
private const val JSON_NAME = "name"
private const val JSON_NO_APPEALS = "no_appeals"
private const val JSON_NOTES = "notes"
private const val JSON_PLEDGE_AMOUNT = "pledge_amount"
private const val JSON_PLEDGE_CURRENCY = "pledge_currency"
private const val JSON_PLEDGE_FREQUENCY = "pledge_frequency"
private const val JSON_PLEDGE_RECEIVED = "pledge_received"
private const val JSON_SEND_NEWSLETTER = "send_newsletter"
private const val JSON_STATUS = "status"
private const val JSON_TAGS = "tag_list"
private const val JSON_WEBSITE = "website"
private const val JSON_STARRED_AT = "starred_at"

@JsonApiType(JSON_API_TYPE_CONTACT)
open class Contact : RealmObject(), UniqueItem, JsonApiModel, ChangeAwareItem, TagsConcern {
    companion object {
        const val JSON_ADDRESSES = "addresses"
        const val JSON_PEOPLE = "people"

        @JvmField
        val JSON_FIELDS_SPARSE = arrayOf(JSON_NAME, JSON_ENVELOPE_GREETING, JSON_ACCOUNT_LIST)
    }

    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType get() = JSON_API_TYPE_CONTACT

    @JsonApiPlaceholder
    override var isPlaceholder = false
    @Ignore
    @JsonApiIgnore
    override var replacePlaceholder = false
    // endregion JsonApiModel

    @Ignore
    @JsonApiAttribute("create_default_person", deserialize = false)
    var createDefaultPerson = false

    @JsonApiAttribute(JSON_GREETING)
    var greeting: String? = null
        set(value) {
            if (field != value) markChanged(JSON_GREETING)
            field = value
        }

    @JsonApiAttribute("church_name")
    @SerializedName("church_name")
    var churchName: String? = null

    @JsonApiAttribute(JSON_ENVELOPE_GREETING)
    @SerializedName(JSON_ENVELOPE_GREETING)
    var envelopeGreeting: String? = null
        set(value) {
            if (field != value) markChanged(JSON_ENVELOPE_GREETING)
            field = value
        }

    @JsonApiAttribute(JSON_LIKELY_TO_GIVE)
    @SerializedName(JSON_LIKELY_TO_GIVE)
    var likelyToGive: String? = null
        set(value) {
            if (field != value) markChanged(JSON_LIKELY_TO_GIVE)
            field = value
        }

    @JsonApiAttribute(JSON_LOCALE)
    var locale: String? = null
        set(value) {
            if (field != value) markChanged(JSON_LOCALE)
            field = value
        }

    @JsonApiAttribute(JSON_NAME)
    var name: String? = null
        set(value) {
            if (field != value) markChanged(JSON_NAME)
            field = value
        }

    @JsonApiAttribute(JSON_NO_APPEALS)
    @SerializedName(JSON_NO_APPEALS)
    var noAppeals: Boolean? = false
        set(value) {
            if (field != value) markChanged(JSON_NO_APPEALS)
            field = value ?: false
        }

    @JsonApiAttribute(JSON_NOTES)
    var notes: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_NOTES)
            field = value
        }

    @JsonApiAttribute(JSON_PLEDGE_AMOUNT)
    @SerializedName(JSON_PLEDGE_AMOUNT)
    var pledgeAmount: String? = null
        set(value) {
            if (field != value) markChanged(JSON_PLEDGE_AMOUNT)
            field = value
        }

    @JsonApiAttribute(JSON_PLEDGE_CURRENCY)
    @SerializedName(JSON_PLEDGE_CURRENCY)
    var pledgeCurrency: String? = null
        set(value) {
            if (field != value) markChanged(JSON_PLEDGE_CURRENCY)
            field = value
        }

    @JsonApiAttribute(JSON_PLEDGE_FREQUENCY)
    @SerializedName(JSON_PLEDGE_FREQUENCY)
    var pledgeFrequency: String? = null
        set(value) {
            if (field != value) markChanged(JSON_PLEDGE_FREQUENCY)
            field = value
        }

    @JsonApiAttribute(JSON_PLEDGE_RECEIVED)
    @SerializedName(JSON_PLEDGE_RECEIVED)
    var pledgeReceived: Boolean? = false
        set(value) {
            if (field != value) markChanged(JSON_PLEDGE_RECEIVED)
            field = value ?: false
        }

    @JsonApiAttribute("pledge_start_date")
    @SerializedName("pledge_start_date")
    private var pledgeStartDate: String? = null

    @JsonApiAttribute(JSON_SEND_NEWSLETTER)
    @SerializedName(JSON_SEND_NEWSLETTER)
    var sendNewsletter: String? = null
        set(value) {
            if (field != value) markChanged(JSON_SEND_NEWSLETTER)
            field = value
        }

    @JsonApiAttribute("square_avatar")
    @SerializedName("square_avatar")
    var squareAvatar: String? = null

    @JsonApiAttribute(JSON_STATUS)
    @SerializedName(JSON_STATUS)
    var status: String? = null
        set(value) {
            if (field != value) markChanged(JSON_STATUS)
            field = value
        }

    @JsonApiAttribute(JSON_STARRED_AT)
    private var starredAt: Date? = null
        set(value) {
            if (field != value) markChanged(JSON_STARRED_AT)
            field = value
            isStarred
        }

    @JsonApiIgnore
    private var taskIds: RealmList<String>? = null

    @JsonApiAttribute(JSON_TAGS)
    override var tags: RealmList<String>? = null
        get() = field ?: RealmList<String>().also { field = it }
        set(value) {
            val sanitized = value?.mapNotNull { sanitizeTag(it) }?.toSet()
            if (field?.toSet().orEmpty() != sanitized.orEmpty()) markChanged(JSON_TAGS)
            field = sanitized?.toRealmList()
            tagsIndex
        }

    @JsonApiAttribute("late_at")
    @Suppress("PrivatePropertyName")
    private var donation_late_at: Date? = null

    @JsonApiAttribute("total_donations")
    @SerializedName("total_donations")
    private var totalDonations: Int = 0

    var timezone: String? = null

    @JsonApiAttribute("uncompleted_tasks_count")
    @SerializedName("uncompleted_tasks_count")
    var uncompletedTasksCount: Int = 0

    @Ignore
    @JsonApiAttribute("donor_accounts")
    private var donorAccounts: List<DonorAccount>? = null
    var donorAccountIds: RealmList<String>? = null
        private set

    @Ignore
    @JsonApiAttribute("last_donation")
    @SerializedName("last_donation")
    private var ignoreLastDonation: StringMapWrapper? = null

    @JsonApiAttribute(JSON_WEBSITE)
    @SerializedName(JSON_WEBSITE)
    var website: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_WEBSITE)
            field = value
        }

    @JsonApiAttribute("created_at")
    @SerializedName("created_at")
    private var createdAt: Date? = null
    @JsonApiAttribute("updated_at")
    @SerializedName("updated_at")
    private var updatedAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_IN_DB_AT)
    @SerializedName(JSON_ATTR_UPDATED_IN_DB_AT)
    var updatedInDatabaseAt: Date? = null

    @JsonApiIgnore
    var lastDesignationId: String? = null
        private set
    @JsonApiIgnore
    private var lastDonationAmount: String? = null
    @JsonApiIgnore
    private var lastDonationDate: String? = null
    @JsonApiIgnore
    private var lastDonationCurrency: String? = null

    @Ignore
    @SerializedName(JSON_LAST_SIX_DONATIONS)
    @JsonApiAttribute(JSON_LAST_SIX_DONATIONS, serialize = false)
    private var donations: List<Donation>? = null

    @JsonApiIgnore
    private var lastSixDonationIds: RealmList<String>? = null

    fun getLastSixDonations(): RealmResults<Donation> = realm {
        where<Donation>()
            .oneOf(DonationFields.ID, lastSixDonationIds?.toTypedArray().orEmpty())
            .sortByDate()
            .findAll()
    }

    // region Generated Attributes
    @JsonApiIgnore
    var isStarred: Boolean = false
        get() = (starredAt != null).also { if (field != it && (!isManaged || realm!!.isInTransaction)) field = it }
        set(value) {
            starredAt = if (value) starredAt ?: Date() else null
        }

    var donationLateAt
        get() = donation_late_at?.toLocalDate()
        @VisibleForTesting
        internal set(value) {
            donation_late_at = value?.toDate()
        }

    val donationLateState: DonationLateState
        get() {
            val daysLate = donationLateAt?.until(LocalDate.now(), ChronoUnit.DAYS) ?: return DonationLateState.ON_TIME
            return when {
                daysLate < 0 -> DonationLateState.ON_TIME
                daysLate < 30 -> DonationLateState.LATE
                daysLate < 60 -> DonationLateState.THIRTY_DAYS_LATE
                daysLate < 90 -> DonationLateState.SIXTY_DAYS_LATE
                else -> DonationLateState.NINETY_DAYS_LATE
            }
        }

    @JsonApiIgnore
    @get:JsonApiPostCreate
    private var tagsIndex: String? = null
        get() = tags?.takeUnless { it.isEmpty() }
            ?.joinToString(PACKED_FIELD_SEPARATOR, PACKED_FIELD_SEPARATOR, PACKED_FIELD_SEPARATOR)
            .also { if (field != it && (!isManaged || realm!!.isInTransaction)) field = it }
    // endregion Generated Attributes

    // region Relationships
    @JsonApiAttribute(JSON_ACCOUNT_LIST)
    var accountList: AccountList? = null

    @Ignore
    @JsonApiAttribute(JSON_ADDRESSES, serialize = false)
    internal var apiAddresses: List<Address>? = null
    @JsonApiIgnore
    @LinkingObjects("contact")
    private val addresses: RealmResults<Address>? = null

    @JvmOverloads
    open fun getAddresses(includeDeleted: Boolean = false) = realm?.getAddresses(includeDeleted)?.forContact(id)

    @Ignore
    @JsonApiAttribute(JSON_PEOPLE, serialize = false)
    internal var apiPeople: List<Person>? = null
    @JsonApiIgnore
    @LinkingObjects("contact")
    private val people: RealmResults<Person>? = null

    @JvmOverloads
    fun getPeople(includeDeleted: Boolean = false) = realm?.getPeople(includeDeleted)?.forContact(id)

    @JsonApiAttribute(JSON_REFERRED_BY, serialize = false)
    var referredBy: RealmList<Contact>? = null
        private set

    // region Local Relationships
    @JsonApiIgnore
    @LinkingObjects("contact")
    private val askedContacts: RealmResults<AskedContact>? = null

    @JsonApiIgnore
    @LinkingObjects("contact")
    private val taskContacts: RealmResults<TaskContact>? = null
    // endregion Local Relationships
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
        if (source is Contact) {
            when (field) {
                // attributes
                JSON_ENVELOPE_GREETING -> envelopeGreeting = source.envelopeGreeting
                JSON_GREETING -> greeting = source.greeting
                JSON_LIKELY_TO_GIVE -> likelyToGive = source.likelyToGive
                JSON_LOCALE -> locale = source.locale
                JSON_NAME -> name = source.name
                JSON_NO_APPEALS -> noAppeals = source.noAppeals
                JSON_NOTES -> notes = source.notes
                JSON_PLEDGE_AMOUNT -> pledgeAmount = source.pledgeAmount
                JSON_PLEDGE_CURRENCY -> pledgeCurrency = source.pledgeCurrency
                JSON_PLEDGE_FREQUENCY -> pledgeFrequency = source.pledgeFrequency
                JSON_PLEDGE_RECEIVED -> pledgeReceived = source.pledgeReceived
                JSON_SEND_NEWSLETTER -> sendNewsletter = source.sendNewsletter
                JSON_STATUS -> status = source.status
                JSON_TAGS -> tags = source.tags
                JSON_WEBSITE -> website = source.website
                JSON_STARRED_AT -> starredAt = source.starredAt
            }
        }
    }
    // endregion ChangeAwareItem

    // region TagsConcern
    override fun onTagsChanged() {
        markChanged(JSON_TAGS)
        tagsIndex
    }
    // endregion TagsConcern

    // region API logic
    @JsonApiPostCreate
    private fun populatePersonContactRelationship() = apiPeople?.forEach {
        it.contact = this
    }

    @JsonApiPostCreate
    private fun populateAddressContactRelationship() = apiAddresses?.forEach { it.contact = this }

    @JsonApiPostCreate
    private fun populateIsStarredAttribute() = isStarred

    @JsonApiPostCreate
    private fun flattenLastSixDonations() {
        lastSixDonationIds = donations?.mapNotNullTo(RealmList()) { it.id }
    }

    @JsonApiPostCreate
    private fun cleanup() {
        ignoreLastDonation?.let {
            lastDesignationId = it.get("designation_account_id")
            lastDonationAmount = it.get("amount")
            lastDonationDate = it.get("donation_date")
            lastDonationCurrency = it.get("currency")
        }

        donorAccountIds = donorAccounts?.mapNotNullTo(RealmList()) { it.id }
    }
    // endregion API logic

    fun getPledgeFrequencyString(stringResolver: StringResolver): String {
        if (TextUtils.isEmpty(pledgeFrequency)) {
            return ""
        }
        val freqValue = java.lang.Float.parseFloat(pledgeFrequency!!)
        return if (freqValue >= 1) {
            stringResolver.getQuantityString(R.plurals.donation_frequency, freqValue.roundToInt())
        } else {
            stringResolver.getQuantityString(R.plurals.donation_frequency_weeks, (freqValue * 4.28).roundToInt())
        }
    }

    fun getLastSixDonationIds(): List<String>? {
        return lastSixDonationIds
    }

    enum class DonationLateState { ON_TIME, LATE, THIRTY_DAYS_LATE, SIXTY_DAYS_LATE, NINETY_DAYS_LATE, ALL_LATE }
}
