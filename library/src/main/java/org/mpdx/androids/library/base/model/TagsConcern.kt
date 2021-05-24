package org.mpdx.androids.library.base.model

import io.realm.RealmList

interface TagsConcern {
    var tags: RealmList<String>?
    fun onTagsChanged() = Unit
}

fun TagsConcern.addTag(tag: String): Boolean {
    tags?.apply {
        sanitizeTag(tag)?.let {
            if (!contains(it)) {
                add(it)
                onTagsChanged()
                return true
            }
        }
    }
    return false
}

fun TagsConcern.removeTag(tag: String): Boolean {
    if (tags?.remove(tag) == true) {
        onTagsChanged()
        return true
    }
    return false
}

@Suppress("unused")
fun TagsConcern.sanitizeTag(tag: String?) = tag?.trim()?.takeIf { it.isNotBlank() }
