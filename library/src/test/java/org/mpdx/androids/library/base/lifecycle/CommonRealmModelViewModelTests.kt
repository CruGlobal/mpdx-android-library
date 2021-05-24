package org.mpdx.base.lifecycle

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import io.realm.MockRealmObjectProxy
import io.realm.RealmModel
import io.realm.internal.RealmObjectProxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mpdx.androids.library.base.lifecycle.RealmModelViewModel
import org.mpdx.base.lifecycle.CommonRealmModelViewModelTests.Model
import org.mpdx.androids.library.base.model.UniqueItem
import java.util.UUID

private const val FIELD_PROPERTY = 1
private const val FIELD_BOOLEAN_PROPERTY = 2
private const val FIELD_NULLABLE_BOOLEAN_PROPERTY = 3
private const val FIELD_RELATED = 4
private const val FIELD_RELATED_PROPERTY = 5

class CommonRealmModelViewModelTests : BaseRealmModelViewModelTests<Model>() {
    override lateinit var viewModel: ModelViewModel

    @Before
    fun setupViewModel() {
        viewModel = ModelViewModel().apply {
            addOnPropertyChangedCallback(changeCallback)
        }
    }

    @Test
    fun testModelBooleanDelegate() {
        val model = createUnmanagedModel()
        model.boolProp = false
        viewModel.model = model

        reset(changeCallback)
        viewModel.boolProp = true
        assertTrue(model.boolProp)
        verify(changeCallback, atLeast(1)).onPropertyChanged(any(), eq(FIELD_BOOLEAN_PROPERTY))
    }

    @Test
    fun testModelNullableBooleanDelegate() {
        val model = createUnmanagedModel()
        model.nullableBoolProperty = null
        viewModel.model = model

        reset(changeCallback)
        assertNull(viewModel.nullableBoolProp)
        viewModel.nullableBoolProp = true
        assertTrue(model.nullableBoolProperty!!)
        verify(changeCallback, atLeast(1)).onPropertyChanged(any(), eq(FIELD_NULLABLE_BOOLEAN_PROPERTY))
    }

    @Test
    fun testModelStringDelegate() {
        val model = createUnmanagedModel()
        val value = UUID.randomUUID().toString()
        viewModel.model = model

        reset(changeCallback)
        viewModel.property = value
        assertEquals(value, model.property)
        verify(changeCallback, atLeast(1)).onPropertyChanged(any(), eq(FIELD_PROPERTY))
    }

    @Test
    fun testRelatedModelCascadePropertyChangeUpHierarchy() {
        val model = createUnmanagedModel()
        val relatedModel = RelatedModel().apply { id = UUID.randomUUID().toString() }
        viewModel.model = model
        viewModel.relatedModels.addModel(relatedModel)
        val relatedViewModel = viewModel.relatedModels.viewModels.first()

        reset(changeCallback)
        relatedViewModel.property = UUID.randomUUID().toString()
        verify(changeCallback).onPropertyChanged(any(), any())
        verify(changeCallback).onPropertyChanged(any(), eq(FIELD_RELATED))
    }

    @Test
    fun verifyRelatedModelsExistingAndAddedModelsShouldBeDistinct() {
        val id = UUID.randomUUID().toString()
        viewModel.relatedModels.existing = listOf(RelatedModel(id))

        viewModel.relatedModels.addModel(RelatedModel(id))
        assertEquals(1, viewModel.relatedModels.size)
        viewModel.relatedModels.deleteModel(id)
        assertEquals(0, viewModel.relatedModels.size)
    }

    @Test
    fun verifyRelatedModelsAddingDeletedModel() {
        val id = UUID.randomUUID().toString()

        viewModel.relatedModels.addModel(RelatedModel(id))
        assertEquals(1, viewModel.relatedModels.size)
        viewModel.relatedModels.deleteModel(id)
        assertEquals(0, viewModel.relatedModels.size)
        viewModel.relatedModels.addModel(RelatedModel(id))
        assertEquals(1, viewModel.relatedModels.size)
    }

    override fun createUnmanagedModel() = Model()
    override fun createManagedModel() = ManagedModel()

    open class Model : RealmModel {
        var boolProp: Boolean = false
        var nullableBoolProperty: Boolean? = null
        var property: String? = ""
    }

    open class RelatedModel(
        override var id: String? = null
    ) : RealmModel, UniqueItem {
        var property: String? = ""
    }

    class ModelViewModel : RealmModelViewModel<Model>() {
        override fun createModel() = Model()

        var boolProp by modelBooleanProperty(FIELD_BOOLEAN_PROPERTY) { it::boolProp }
        var nullableBoolProp by modelNullableBooleanProperty(FIELD_NULLABLE_BOOLEAN_PROPERTY, null) {
            it::nullableBoolProperty
        }
        var property by modelStringProperty(FIELD_PROPERTY) { it::property }

        val relatedModels = RelatedModels(FIELD_RELATED) {
            RelatedModelViewModel().also {
                it.allowNullModel = allowNullModel
                it.forceUnmanaged = forceUnmanaged
            }
        }
    }

    class RelatedModelViewModel : RealmModelViewModel<RelatedModel>() {
        override fun createModel() = RelatedModel()

        var property by modelStringProperty(FIELD_RELATED_PROPERTY) { it::property }
    }

    inner class ManagedModel : Model(), RealmObjectProxy by MockRealmObjectProxy(realm)
}
