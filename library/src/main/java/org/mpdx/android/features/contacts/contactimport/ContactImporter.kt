package org.mpdx.android.features.contacts.contactimport

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.google.firebase.perf.metrics.AddTrace
import org.ccci.gto.android.common.util.database.getInt
import org.ccci.gto.android.common.util.database.getLong
import org.ccci.gto.android.common.util.database.getString
import org.ccci.gto.android.common.util.database.mapTo
import org.mpdx.android.features.analytics.PERF_IMPORT_CONTACT
import org.mpdx.android.features.contacts.contactimport.model.ImportedContact
import org.mpdx.android.features.contacts.model.Address
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.EmailAddress
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.model.PhoneNumber
import org.mpdx.android.utils.toLocalDateOrNull
import org.mpdx.android.utils.toMonthDayOrNull

@AddTrace(name = PERF_IMPORT_CONTACT)
internal fun ContentResolver.importContact(uri: Uri, importPersonOnly: Boolean = false): ImportedContact? {
    val contactId = getContactId(uri) ?: return null
    val contact = Contact().apply { trackingChanges = true }
    val person = Person().apply { trackingChanges = true }

    readFirstRow(nameQuery(contactId)) {
        person.firstName = getString(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
        person.middleName = getString(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)
        person.lastName = getString(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)
        person.title = getString(ContactsContract.CommonDataKinds.StructuredName.PREFIX)
        person.suffix = getString(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)

        contact.name = arrayOf(person.lastName, person.firstName).filterNotNull().joinToString(", ")
        contact.greeting = person.firstName
        contact.envelopeGreeting = arrayOf(person.firstName, person.lastName).filterNotNull().joinToString(" ")
    }
    readFirstRow(organizationQuery(contactId)) {
        person.employer = getString(ContactsContract.CommonDataKinds.Organization.COMPANY)
    }
    readFirstRow(birthdayQuery(contactId)) {
        val date = getString(ContactsContract.CommonDataKinds.Event.START_DATE)
        date?.toMonthDayOrNull()?.let { person.birthday = it }
        date?.toLocalDateOrNull()?.let { person.birthDate = it }
    }
    readFirstRow(anniversaryQuery(contactId)) {
        val date = getString(ContactsContract.CommonDataKinds.Event.START_DATE)
        date?.toMonthDayOrNull()?.let { person.anniversary = it }
        date?.toLocalDateOrNull()?.let { person.anniversaryDate = it }
    }

    if (!importPersonOnly) {
        readFirstRow(websiteQuery(contactId)) {
            contact.website = getString(ContactsContract.CommonDataKinds.Website.URL)
        }
    }

    return ImportedContact(
        contact, person,
        addresses = if (!importPersonOnly) readAddresses(contactId) else null,
        emailAddresses = readEmailAddresses(contactId),
        phoneNumbers = readPhoneNumbers(contactId)
    )
}

private fun ContentResolver.getContactId(uri: Uri): Long? =
    readFirstRow(query(uri, arrayOf(ContactsContract.Contacts._ID), null, null, null)) {
        getLong(ContactsContract.Contacts._ID)
    }

private fun <T> readFirstRow(cursor: Cursor?, block: Cursor.() -> T) =
    cursor?.use { if (it.moveToFirst()) it.block() else null }

private fun ContentResolver.readEmailAddresses(contactId: Long): List<EmailAddress> {
    val emails = mutableListOf<EmailAddress>()
    // TODO: filter duplicates
    emailAddressQuery(contactId)?.use {
        it.mapTo(emails) { c ->
            return@mapTo EmailAddress().apply {
                trackingChanges = true
                email = c.getString(ContactsContract.CommonDataKinds.Email.DATA)
                location = when (c.getInt(ContactsContract.CommonDataKinds.Email.TYPE)) {
                    ContactsContract.CommonDataKinds.Email.TYPE_HOME -> "Home"
                    ContactsContract.CommonDataKinds.Email.TYPE_WORK -> "Work"
                    else -> "Other"
                }
            }
        }
    }
    (emails.firstOrNull { it.location == "Home" } ?: emails.firstOrNull { it.location == "Work" })?.isPrimary = true
    return emails
}

private fun ContentResolver.readPhoneNumbers(contactId: Long): List<PhoneNumber> {
    val numbers = mutableListOf<PhoneNumber>()
    // TODO: normalize & filter duplicates
    phoneNumberQuery(contactId)?.use {
        it.mapTo(numbers) { c ->
            return@mapTo PhoneNumber().apply {
                trackingChanges = true
                number = c.getString(ContactsContract.CommonDataKinds.Phone.NUMBER)
                location = when (c.getInt(ContactsContract.CommonDataKinds.Phone.TYPE)) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                    else -> "Other"
                }
            }
        }
    }
    (numbers.firstOrNull { it.location == "Mobile" } ?: numbers.firstOrNull { it.location == "Home" })?.isPrimary = true
    return numbers
}

private fun ContentResolver.readAddresses(contactId: Long): List<Address> {
    val addresses = mutableListOf<Address>()
    addressQuery(contactId)?.use {
        it.mapTo(addresses) { c ->
            return@mapTo Address().apply {
                trackingChanges = true
                street = c.getString(ContactsContract.CommonDataKinds.StructuredPostal.STREET)
                city = c.getString(ContactsContract.CommonDataKinds.StructuredPostal.CITY)
                state = c.getString(ContactsContract.CommonDataKinds.StructuredPostal.REGION)
                postalCode = c.getString(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)
                country = c.getString(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)
                location = when (c.getInt(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)) {
                    ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> "Home"
                    ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> "Work"
                    else -> "Other"
                }
            }
        }
    }
    addresses.firstOrNull { it.location == "Home" }?.isPrimary = true
    return addresses
}

// region Queries
private const val QUERY_CONTACT = "${ContactsContract.Data.CONTACT_ID} = ?"
private const val QUERY_MIME_TYPE = "$QUERY_CONTACT AND ${ContactsContract.Data.MIMETYPE} = ?"
private const val QUERY_EVENT = "$QUERY_MIME_TYPE AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"

private fun ContentResolver.mimeTypeQuery(contactId: Long, type: String) =
    query(ContactsContract.Data.CONTENT_URI, null, QUERY_MIME_TYPE, arrayOf(contactId.toString(), type), null)

private fun ContentResolver.eventQuery(contactId: Long, type: Int) = query(
    ContactsContract.Data.CONTENT_URI, null, QUERY_EVENT,
    arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE, type.toString()), null
)

private fun ContentResolver.nameQuery(contactId: Long) =
    mimeTypeQuery(contactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)

private fun ContentResolver.organizationQuery(contactId: Long) =
    mimeTypeQuery(contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)

private fun ContentResolver.addressQuery(contactId: Long) =
    mimeTypeQuery(contactId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)

private fun ContentResolver.birthdayQuery(contactId: Long) =
    eventQuery(contactId, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)

private fun ContentResolver.anniversaryQuery(contactId: Long) =
    eventQuery(contactId, ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY)

private fun ContentResolver.emailAddressQuery(contactId: Long) =
    query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, QUERY_CONTACT, arrayOf(contactId.toString()), null)

private fun ContentResolver.phoneNumberQuery(contactId: Long) =
    query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, QUERY_CONTACT, arrayOf(contactId.toString()), null)

private fun ContentResolver.websiteQuery(contactId: Long) =
    mimeTypeQuery(contactId, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
// endregion Queries
