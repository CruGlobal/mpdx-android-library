package org.mpdx.androids.library.base.model

interface UniqueItem {
    val id: String?

    companion object {
        const val FIELD_ID = "id"
    }
}

fun <T : UniqueItem> MutableCollection<T>.addUnique(item: T): Boolean =
    if (none { it.id == item.id }) add(item) else false
