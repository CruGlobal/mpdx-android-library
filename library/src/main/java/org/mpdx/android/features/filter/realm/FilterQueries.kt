package org.mpdx.android.features.filter.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.mpdx.android.features.filter.model.Filter
import org.mpdx.android.features.filter.model.Filter.Companion.CONTAINER_CONTACT
import org.mpdx.android.features.filter.model.Filter.Companion.CONTAINER_NOTIFICATION
import org.mpdx.android.features.filter.model.Filter.Companion.CONTAINER_TASK
import org.mpdx.android.features.filter.model.FilterFields

fun Realm.getFilters() = where<Filter>()

fun Realm.getContactFilters() = getFilters().forContainer(CONTAINER_CONTACT)
fun Realm.getNotificationFilters() = getFilters().forContainer(CONTAINER_NOTIFICATION)
fun Realm.getTaskFilters() = getFilters().forContainer(CONTAINER_TASK)

fun RealmQuery<Filter>.forContainer(container: Int?): RealmQuery<Filter> = equalTo(FilterFields.CONTAINER, container)
fun RealmQuery<Filter>.type(type: Filter.Type?): RealmQuery<Filter> = equalTo(FilterFields._TYPE, type?.ordinal)
fun RealmQuery<Filter>.key(key: String?): RealmQuery<Filter> = equalTo(FilterFields.KEY, key)
fun RealmQuery<Filter>.isEnabled(): RealmQuery<Filter> = equalTo(FilterFields.IS_ENABLED, true)

fun RealmQuery<Filter>.sortByName(): RealmQuery<Filter> = sort(FilterFields.LABEL)
