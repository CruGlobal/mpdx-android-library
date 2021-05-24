package org.mpdx.base.model

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasSize
import org.junit.Test
import org.mpdx.androids.library.base.model.ChangeAwareItem

class ChangeAwareItemTests {
    @Test
    fun verifyMarkChangedWhenTrackingOnly() {
        val model = SimpleChangeAwareModel()

        model.trackingChanges = true
        model.markChanged("tracked")
        model.trackingChanges = false
        model.markChanged("untracked")
        assertThat(model.changedFields.toList(), allOf(hasSize(1), contains("tracked")))
    }

    @Test
    fun verifyMergeChangedFields() {
        val source = SimpleChangeAwareModel()
        source.trackingChanges = true
        source.markChanged("keep_1")
        source.markChanged("discard_1")
        source.markChanged("keep_2")

        val target = MergeChangedFieldsTestModel()
        target.mergeChangedFields(source)
        assertThat(target.calledFor, allOf(hasSize(3), contains("keep_1", "discard_1", "keep_2")))
        assertThat(target.changedFields.toList(), allOf(hasSize(2), contains("keep_1", "keep_2")))
    }

    @Test
    fun verifyClearChangedFieldsMatching() {
        val original = SimpleChangeAwareModel()
        original.trackingChanges = true
        original.markChanged("matches_1")
        original.markChanged("different_1")
        original.markChanged("matches_2")
        val target = ClearChangedFieldsMatchingTestModel()
        target.trackingChanges = true
        target.changedFieldsStr = original.changedFieldsStr
        target.markChanged("new_1")

        target.clearChangedFieldsMatching(original)
        assertThat(target.calledFor, allOf(hasSize(3), contains("matches_1", "different_1", "matches_2")))
        assertThat(target.changedFields.toList(), allOf(hasSize(2), contains("different_1", "new_1")))
    }
}

open class SimpleChangeAwareModel : ChangeAwareItem {
    override var isNew = false
    override var isDeleted = false
    override var trackingChanges = false
    override var changedFieldsStr = ""
}

private class MergeChangedFieldsTestModel : SimpleChangeAwareModel() {
    val calledFor = mutableListOf<String>()
    override fun mergeChangedField(source: ChangeAwareItem, field: String) {
        calledFor.add(field)
        if (field.startsWith("keep_")) markChanged(field)
    }
}

private class ClearChangedFieldsMatchingTestModel : SimpleChangeAwareModel() {
    val calledFor = mutableListOf<String>()
    override fun doesFieldMatch(original: ChangeAwareItem, field: String): Boolean {
        calledFor.add(field)
        return field.startsWith("matches")
    }
}
