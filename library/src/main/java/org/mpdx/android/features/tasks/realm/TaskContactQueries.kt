package org.mpdx.android.features.tasks.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import java.util.UUID
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.model.TaskContact
import org.mpdx.android.features.tasks.model.TaskContactFields

@JvmOverloads
fun Realm.getTaskContacts(includeDeleted: Boolean = false) = where<TaskContact>()
    .includeDeleted(includeDeleted)

fun Realm.getTaskContact(taskId: String?, contactId: String?) = getTaskContacts()
    .forTask(taskId)
    .forContact(contactId)

fun RealmQuery<TaskContact>.forTask(taskId: String?): RealmQuery<TaskContact> =
    equalTo(TaskContactFields.TASK.ID, taskId)

fun RealmQuery<TaskContact>.forContact(contactId: String?): RealmQuery<TaskContact> =
    equalTo(TaskContactFields.CONTACT.ID, contactId)

fun createTaskContact(task: Task, contact: Contact) = TaskContact().apply {
    id = UUID.randomUUID().toString()
    isNew = true
    this.contact = contact
    this.task = task
}
