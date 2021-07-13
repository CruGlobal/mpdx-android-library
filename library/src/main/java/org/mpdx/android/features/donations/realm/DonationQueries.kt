package org.mpdx.android.features.donations.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort
import io.realm.kotlin.where
import org.mpdx.android.features.appeals.model.AppealFields
import org.mpdx.android.features.contacts.model.ContactFields
import org.mpdx.android.features.donations.model.Donation
import org.mpdx.android.features.donations.model.DonationFields
import org.mpdx.android.utils.toDate
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

fun Realm.getDonations() = where<Donation>()
fun Realm.getDonation(id: String?): RealmQuery<Donation> = getDonations().equalTo(DonationFields.ID, id)

fun RealmQuery<Donation>.forAccountList(accountListId: String?): RealmQuery<Donation> =
    equalTo(DonationFields.ACCOUNT_LIST.ID, accountListId)
fun RealmQuery<Donation>.forContact(contactId: String?): RealmQuery<Donation> =
    equalTo("${DonationFields.DONOR_ACCOUNT.CONTACTS}.${ContactFields.ID}", contactId)
fun RealmQuery<Donation>.forAppeal(appealId: String?): RealmQuery<Donation> =
    equalTo("${DonationFields.PLEDGE.APPEAL}.${AppealFields.ID}", appealId)

fun RealmQuery<Donation>.forMonth(month: YearMonth): RealmQuery<Donation> =
    between(month.atDay(1), month.plusMonths(1).atDay(1).minusDays(1))

fun RealmQuery<Donation>.between(from: LocalDate, to: LocalDate): RealmQuery<Donation> =
    between(DonationFields.DONATION_DATE, from.toDate(), to.toDate())

fun RealmQuery<Donation>.sortByDate(): RealmQuery<Donation> = sort(DonationFields.DONATION_DATE, Sort.DESCENDING)
