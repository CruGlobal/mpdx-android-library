package org.mpdx.android.core.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.mpdx.android.core.model.Tag
import org.mpdx.android.core.model.TagFields

fun Realm.getTags(): RealmQuery<Tag> = where<Tag>().sort(TagFields.NAME)

fun Realm.getContactTagsFor(accountListId: String?): RealmQuery<Tag> =
    getTags().equalTo(TagFields.CONTACT_TAG_FOR.ID, accountListId)

fun Realm.getTaskTagsFor(accountListId: String?): RealmQuery<Tag> =
    getTags().equalTo(TagFields.TASK_TAG_FOR.ID, accountListId)
