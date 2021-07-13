package org.mpdx.android.features.contacts.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import java.util.Date
import java.util.UUID
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.features.contacts.model.ContactFields
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.model.PhoneNumber
import org.mpdx.android.features.contacts.model.PhoneNumberFields
import org.mpdx.android.features.tasks.model.TaskFields

fun Realm.getPhoneNumbers(includeDeleted: Boolean = false) = where<PhoneNumber>()
    .includeDeleted(includeDeleted)

fun RealmQuery<PhoneNumber>.forTask(taskId: String?): RealmQuery<PhoneNumber> =
    equalTo("${PhoneNumberFields.PERSON.CONTACT}.${ContactFields.TASK_CONTACTS.TASK}.${TaskFields.ID}", taskId)

fun createPhoneNumber(person: Person? = null) = PhoneNumber().apply {
    id = UUID.randomUUID().toString()
    isNew = true
    createdAt = Date()
    this.person = person
}
