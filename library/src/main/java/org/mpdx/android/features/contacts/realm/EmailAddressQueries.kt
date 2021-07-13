package org.mpdx.android.features.contacts.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import java.util.Date
import java.util.UUID
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.features.contacts.model.ContactFields
import org.mpdx.android.features.contacts.model.EmailAddress
import org.mpdx.android.features.contacts.model.EmailAddressFields
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.tasks.model.TaskFields

fun Realm.getEmailAddresses(includeDeleted: Boolean = false) = where<EmailAddress>()
    .includeDeleted(includeDeleted)

fun RealmQuery<EmailAddress>.forContact(contactId: String?): RealmQuery<EmailAddress> =
    equalTo("${EmailAddressFields.PERSON.CONTACT}.${ContactFields.ID}", contactId)
fun RealmQuery<EmailAddress>.forTask(taskId: String?): RealmQuery<EmailAddress> =
    equalTo("${EmailAddressFields.PERSON.CONTACT}.${ContactFields.TASK_CONTACTS.TASK}.${TaskFields.ID}", taskId)

fun createEmailAddress(person: Person? = null) = EmailAddress().apply {
    id = UUID.randomUUID().toString()
    isNew = true
    createdAt = Date()
    this.person = person
}
