package org.mpdx.android.features.tasks.realm

import io.realm.Realm
import io.realm.kotlin.where
import java.util.Date
import java.util.UUID
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.tasks.model.Comment
import org.mpdx.android.features.tasks.model.Task

fun Realm.getDirtyComments() = where<Comment>().isDirty()

@JvmOverloads
fun createComment(task: Task, person: Person? = null) = Comment().apply {
    id = UUID.randomUUID().toString()
    isNew = true
    createdAt = Date()
    this.task = task
    this.person = person
}
