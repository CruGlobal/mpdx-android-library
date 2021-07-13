package org.mpdx.android.features.contacts.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.oneOf
import io.realm.kotlin.where
import java.util.UUID
import org.mpdx.android.base.realm.PACKED_FIELD_SEPARATOR
import org.mpdx.android.base.realm.between
import org.mpdx.android.base.realm.greaterThan
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.base.realm.lessThanOrEqualTo
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.Contact.DonationLateState
import org.mpdx.android.features.contacts.model.ContactFields
import org.mpdx.android.features.filter.FILTER_CONTACT_STATUS_NONE
import org.mpdx.android.features.filter.HIDDEN_STATUSES
import org.mpdx.android.features.filter.model.Filter
import org.mpdx.android.features.filter.model.Filter.Type.ACTION_TYPE
import org.mpdx.android.features.filter.model.Filter.Type.CONTACT_CHURCH
import org.mpdx.android.features.filter.model.Filter.Type.CONTACT_CITY
import org.mpdx.android.features.filter.model.Filter.Type.CONTACT_LIKELY_TO_GIVE
import org.mpdx.android.features.filter.model.Filter.Type.CONTACT_REFERRER
import org.mpdx.android.features.filter.model.Filter.Type.CONTACT_STATE
import org.mpdx.android.features.filter.model.Filter.Type.CONTACT_STATUS
import org.mpdx.android.features.filter.model.Filter.Type.CONTACT_TAGS
import org.mpdx.android.features.filter.model.Filter.Type.CONTACT_TIMEZONE
import org.mpdx.android.features.filter.model.Filter.Type.NOTIFICATION_TYPES
import org.mpdx.android.features.filter.model.Filter.Type.TASK_TAGS
import org.mpdx.android.features.tasks.model.TaskFields
import org.mpdx.android.utils.toDate
import org.threeten.bp.LocalDate

fun Realm.getContacts(includeDeleted: Boolean = false) = where<Contact>()
    .includeDeleted(includeDeleted)

fun Realm.getContacts(accountListId: String?, includeDeleted: Boolean = false) = getContacts(includeDeleted)
    .forAccountList(accountListId)

fun Realm.getDirtyContacts() = where<Contact>().isDirty()

fun Realm.getContact(id: String?): RealmQuery<Contact> = where<Contact>().equalTo(ContactFields.ID, id)

fun RealmQuery<Contact>.hasName(): RealmQuery<Contact> = beginGroup()
    .isNotNull(ContactFields.NAME).and().isNotEmpty(ContactFields.NAME)
    .endGroup()

fun RealmQuery<Contact>.status(status: String): RealmQuery<Contact> = equalTo(ContactFields.STATUS, status)
fun RealmQuery<Contact>.hasNoStatus(): RealmQuery<Contact> =
    beginGroup().isNull(ContactFields.STATUS).or().isEmpty(ContactFields.STATUS).endGroup()
fun RealmQuery<Contact>.hasVisibleStatus(): RealmQuery<Contact> =
    beginGroup().not().oneOf(ContactFields.STATUS, HIDDEN_STATUSES.toTypedArray()).endGroup()

fun RealmQuery<Contact>.forAccountList(accountListId: String?): RealmQuery<Contact> =
    equalTo(ContactFields.ACCOUNT_LIST.ID, accountListId)
fun RealmQuery<Contact>.forTask(taskId: String?): RealmQuery<Contact> =
    equalTo("${ContactFields.TASK_CONTACTS.TASK}.${TaskFields.ID}", taskId)

fun RealmQuery<Contact>.isReferral() = isNotNull(ContactFields.REFERRED_BY.ID)

fun RealmQuery<Contact>.receivedGift(received: Boolean): RealmQuery<Contact> =
    equalTo(ContactFields.PLEDGE_RECEIVED, received)

fun RealmQuery<Contact>.applyFilters(filters: List<Filter>?) = apply {
    if (filters.isNullOrEmpty()) return@apply

    beginGroup()
    filters.groupBy { it.type }.forEach { (type, filters) ->
        when (type) {
            CONTACT_STATUS -> {
                beginGroup()
                val statuses = filters.mapNotNullTo(mutableSetOf()) { it.key }
                if (statuses.remove(FILTER_CONTACT_STATUS_NONE)) hasNoStatus().or()
                oneOf(ContactFields.STATUS, filters.mapNotNull { it.key }.toTypedArray())
                endGroup()
            }
            CONTACT_CHURCH -> oneOf(ContactFields.CHURCH_NAME, filters.mapNotNull { it.key }.toTypedArray())
            CONTACT_LIKELY_TO_GIVE -> oneOf(ContactFields.LIKELY_TO_GIVE, filters.mapNotNull { it.key }.toTypedArray())
            CONTACT_TIMEZONE -> oneOf(ContactFields.TIMEZONE, filters.mapNotNull { it.key }.toTypedArray())
            CONTACT_CITY -> oneOf(ContactFields.ADDRESSES.CITY, filters.mapNotNull { it.key }.toTypedArray())
            CONTACT_STATE -> oneOf(ContactFields.ADDRESSES.STATE, filters.mapNotNull { it.key }.toTypedArray())
            CONTACT_REFERRER -> oneOf(ContactFields.REFERRED_BY.ID, filters.mapNotNull { it.key }.toTypedArray())
            CONTACT_TAGS -> {
                beginGroup()
                filters.mapNotNull { it.key }.forEachIndexed { i, tag ->
                    if (i != 0) or()
                    contains(ContactFields.TAGS_INDEX, "$PACKED_FIELD_SEPARATOR$tag$PACKED_FIELD_SEPARATOR")
                }
                endGroup()
            }
            TASK_TAGS -> Unit
            ACTION_TYPE -> Unit
            NOTIFICATION_TYPES -> Unit
            null -> Unit
        }
    }
    endGroup()
}

fun RealmQuery<Contact>.donationLateState(state: DonationLateState?): RealmQuery<Contact> {
    val today = LocalDate.now()
    return when (state) {
        DonationLateState.ON_TIME, null -> beginGroup()
            .isNull(ContactFields.DONATION_LATE_AT).or().greaterThan(ContactFields.DONATION_LATE_AT, today)
            .endGroup()
        DonationLateState.LATE -> between(ContactFields.DONATION_LATE_AT, today.minusDays(29), today)
        DonationLateState.THIRTY_DAYS_LATE ->
            between(ContactFields.DONATION_LATE_AT, today.minusDays(59), today.minusDays(30))
        DonationLateState.SIXTY_DAYS_LATE ->
            between(ContactFields.DONATION_LATE_AT, today.minusDays(89), today.minusDays(60))
        DonationLateState.NINETY_DAYS_LATE -> lessThanOrEqualTo(ContactFields.DONATION_LATE_AT, today.minusDays(90))
        DonationLateState.ALL_LATE -> receivedGift(true)
            .lessThan(ContactFields.DONATION_LATE_AT, today.minusDays(29).toDate())
    }
}

fun RealmQuery<Contact>.sortByName(): RealmQuery<Contact> = sort(ContactFields.NAME)

fun createContact(accountList: AccountList? = null) = Contact().apply {
    id = UUID.randomUUID().toString()
    isNew = true
    this.accountList = accountList
}
