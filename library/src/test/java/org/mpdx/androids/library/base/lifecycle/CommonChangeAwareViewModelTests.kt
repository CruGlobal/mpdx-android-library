package org.mpdx.base.lifecycle

import io.realm.MockRealmObjectProxy
import io.realm.RealmModel
import io.realm.internal.RealmObjectProxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mpdx.androids.library.base.lifecycle.ChangeAwareViewModel
import org.mpdx.base.lifecycle.CommonChangeAwareViewModelTests.Model
import org.mpdx.androids.library.base.model.ChangeAwareItem
import java.util.UUID

private const val FIELD_PROPERTY = 1

class CommonChangeAwareViewModelTests : BaseChangeAwareViewModelTests<Model>() {
    override lateinit var viewModel: ModelViewModel

    @Before
    fun setupViewModel() {
        viewModel = ModelViewModel().apply {
            addOnPropertyChangedCallback(changeCallback)
        }
    }

    @Test
    fun testTrackingChanges() {
        val property = UUID.randomUUID().toString()
        viewModel.trackingChanges = true

        viewModel.model = createUnmanagedModel()
        assertTrue(viewModel.model!!.trackingChanges)
        viewModel.property = property
        viewModel.model = createUnmanagedModel()
        assertEquals(property, viewModel.property)
    }

    @Test
    fun testTrackingChangesWithNullObjectWhileDisallowingNull() {
        val property = UUID.randomUUID().toString()
        viewModel.allowNullModel = false
        viewModel.trackingChanges = true

        assertTrue(viewModel.model!!.trackingChanges)
        viewModel.property = property
        viewModel.model = createUnmanagedModel()
        assertEquals(property, viewModel.property)
    }

    @Test
    fun testNotTrackingChanges() {
        val property = UUID.randomUUID().toString()
        viewModel.trackingChanges = false

        viewModel.model = createUnmanagedModel()
        assertFalse(viewModel.model!!.trackingChanges)
        viewModel.property = property
        viewModel.model = createUnmanagedModel()
        assertNotEquals(property, viewModel.property)
    }

    override fun createUnmanagedModel() = Model()
    override fun createManagedModel() = ManagedModel()

    open class Model : ChangeAwareItem, RealmModel {
        var property: String? = ""
            set(value) {
                if (value != field) markChanged("property")
                field = value
            }

        override var isNew = false
        override var isDeleted = false
        override var trackingChanges = false
        override var changedFieldsStr = ""
        override fun mergeChangedField(source: ChangeAwareItem, field: String) {
            if (source is Model && field == "property") property = source.property
        }
    }

    class ModelViewModel : ChangeAwareViewModel<Model>() {
        override fun createModel() = Model()

        var property by modelStringProperty(FIELD_PROPERTY) { it::property }
    }

    inner class ManagedModel : Model(), RealmObjectProxy by MockRealmObjectProxy(realm)
}
