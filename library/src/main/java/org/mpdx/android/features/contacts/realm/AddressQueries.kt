package org.mpdx.android.features.contacts.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort
import io.realm.kotlin.where
import java.util.Date
import java.util.UUID
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.features.contacts.model.Address
import org.mpdx.android.features.contacts.model.AddressFields
import org.mpdx.android.features.contacts.model.Contact

fun Realm.getAddresses(includeDeleted: Boolean = false) = where<Address>().includeDeleted(includeDeleted)

fun RealmQuery<Address>.forContact(contactId: String?): RealmQuery<Address> =
    equalTo(AddressFields.CONTACT.ID, contactId)

fun RealmQuery<Address>.sortByCreated(): RealmQuery<Address> =
    sort(AddressFields.CREATED_AT, Sort.ASCENDING, AddressFields.ID, Sort.ASCENDING)
fun RealmQuery<Address>.sortByPrimary(): RealmQuery<Address> = sort(
    arrayOf(AddressFields.IS_PRIMARY, AddressFields.CREATED_AT, AddressFields.ID),
    arrayOf(Sort.DESCENDING, Sort.ASCENDING, Sort.ASCENDING)
)

fun createAddress(contact: Contact? = null) = Address().apply {
    id = UUID.randomUUID().toString()
    isNew = true
    createdAt = Date()
    this.contact = contact
}
