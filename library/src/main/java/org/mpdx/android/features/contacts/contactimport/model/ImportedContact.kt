package org.mpdx.android.features.contacts.contactimport.model

import org.mpdx.android.features.contacts.model.Address
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.EmailAddress
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.model.PhoneNumber

class ImportedContact(
    val contact: Contact,
    val person: Person,
    val addresses: List<Address>?,
    val emailAddresses: List<EmailAddress>,
    val phoneNumbers: List<PhoneNumber>
)
