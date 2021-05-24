package org.mpdx.base.lifecycle

import io.realm.RealmModel
import io.realm.kotlin.isManaged
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mpdx.androids.library.base.model.ChangeAwareItem

abstract class BaseChangeAwareViewModelTests<T> :
    BaseRealmModelViewModelTests<T>() where T : RealmModel, T : ChangeAwareItem {
    @Test
    fun testResetTrackingChangesForUnmanagedObject() {
        val model = createUnmanagedModel()
        model.changedFieldsStr = "changed"
        viewModel.model = model
        assertThat(model.changedFields.toList(), empty())
        assertThat(viewModel.model!!.changedFields.toList(), empty())
        assertFalse(viewModel.model!!.isManaged())
    }

    @Test
    fun testPreserveTrackingChangesForManagedObject() {
        val model = createManagedModel()
        model.changedFieldsStr = "changed"
        viewModel.model = model
        assertThat(model.changedFields.toList(), contains("changed"))
        assertThat(viewModel.model!!.changedFields.toList(), contains("changed"))
        assertTrue(viewModel.model!!.isManaged())
    }
}
