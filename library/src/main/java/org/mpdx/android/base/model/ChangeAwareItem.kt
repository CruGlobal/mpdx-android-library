package org.mpdx.android.base.model

interface ChangeAwareItem {
    var isNew: Boolean
    var isDeleted: Boolean

    var trackingChanges: Boolean
    var changedFieldsStr: String
    val hasChangedFields get() = changedFields.any()

    fun markChanged(field: String) {
        if (trackingChanges) changedFieldsStr = "$changedFieldsStr,$field"
    }

    val changedFields get() = changedFieldsStr.splitToSequence(",").filter { it.isNotEmpty() }.distinct()
    fun mergeChangedField(source: ChangeAwareItem, field: String) = Unit
    fun mergeChangedFields(source: ChangeAwareItem) {
        trackingChanges = true
        source.changedFields.forEach { mergeChangedField(source, it) }
        trackingChanges = false
    }

    fun doesFieldMatch(original: ChangeAwareItem, field: String) = false
    fun clearChangedFieldsMatching(original: ChangeAwareItem) {
        val toClear = original.changedFields.filter { doesFieldMatch(original, it) }.toSet()
        if (toClear.isNotEmpty()) changedFieldsStr = changedFields.filterNot { toClear.contains(it) }.joinToString(",")
    }

    fun clearChanged(field: String) {
        changedFieldsStr = changedFields.filterNot { it == field }.joinToString(",")
    }

    companion object {
        const val FIELD_NEW = "isNew"
        const val FIELD_DELETED = "isDeleted"
        const val FIELD_CHANGED = "changedFieldsStr"
    }
}
