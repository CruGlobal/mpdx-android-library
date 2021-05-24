package org.mpdx.androids.library.base.realm

import io.realm.RealmModel
import io.realm.RealmQuery
import org.mpdx.androids.library.base.model.ChangeAwareItem

fun <E : ChangeAwareItem> RealmQuery<E>.isDirty(): RealmQuery<E> = beginGroup()
    .equalTo(ChangeAwareItem.FIELD_NEW, true).or()
    .isNotEmpty(ChangeAwareItem.FIELD_CHANGED).or()
    .equalTo(ChangeAwareItem.FIELD_DELETED, true)
    .endGroup()

fun <E : RealmModel> RealmQuery<E>.isDirty(prefix: String): RealmQuery<E> = beginGroup()
    .equalTo("$prefix.${ChangeAwareItem.FIELD_NEW}", true).or()
    .isNotEmpty("$prefix.${ChangeAwareItem.FIELD_CHANGED}").or()
    .equalTo("$prefix.${ChangeAwareItem.FIELD_DELETED}", true)
    .endGroup()

fun <E : ChangeAwareItem> RealmQuery<E>.isNotDeleted(): RealmQuery<E> = equalTo(ChangeAwareItem.FIELD_DELETED, false)

fun <E : ChangeAwareItem> RealmQuery<E>.includeDeleted(include: Boolean) = if (include) this else isNotDeleted()
