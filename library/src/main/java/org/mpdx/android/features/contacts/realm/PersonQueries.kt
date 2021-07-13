package org.mpdx.android.features.contacts.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import java.util.Date
import java.util.UUID
import org.mpdx.android.base.realm.between
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.core.model.AccountListFields
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.model.PersonFields
import org.threeten.bp.MonthDay

fun Realm.getPeople(includeDeleted: Boolean = false) = where<Person>().includeDeleted(includeDeleted)
fun Realm.getDirtyPeople() = where<Person>().isDirty()
fun Realm.getPerson(id: String?): RealmQuery<Person> = where<Person>().equalTo(PersonFields.ID, id)

fun RealmQuery<Person>.forAccountList(accountListId: String?): RealmQuery<Person> =
    equalTo("${PersonFields.CONTACT.ACCOUNT_LIST}.${AccountListFields.ID}", accountListId)
fun RealmQuery<Person>.forContact(contactId: String?): RealmQuery<Person> = equalTo(PersonFields.CONTACT.ID, contactId)

fun RealmQuery<Person>.birthdayBetween(from: MonthDay, to: MonthDay) =
    between(PersonFields.BIRTHDAY_DAY_OF_YEAR, from, to)

fun RealmQuery<Person>.anniversaryBetween(from: MonthDay, to: MonthDay) =
    between(PersonFields.ANNIVERSARY_DAY_OF_YEAR, from, to)

fun createPerson(contact: Contact? = null) = Person().apply {
    id = UUID.randomUUID().toString()
    isNew = true
    createdAt = Date()
    this.contact = contact
}
