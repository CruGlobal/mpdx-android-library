package org.mpdx.android.features.tasks.model

import com.squareup.moshi.JsonClass
import io.realm.RealmModel
import io.realm.annotations.RealmClass

@JvmField
val COMPARATOR_OVERDUE_TASKS = compareByDescending<OverdueTask> { it.count }.thenBy(nullsLast()) { it.label }

@RealmClass
@JsonClass(generateAdapter = true)
open class OverdueTask : RealmModel {
    var label: String? = null
    var count: Int = 0
}
