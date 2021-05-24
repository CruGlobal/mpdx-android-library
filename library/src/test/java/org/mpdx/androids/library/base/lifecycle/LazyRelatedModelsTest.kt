package org.mpdx.base.lifecycle

import androidx.databinding.Observable
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import io.realm.RealmModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mpdx.androids.library.base.lifecycle.RealmModelViewModel
import org.mpdx.androids.library.base.model.UniqueItem

class LazyRelatedModelsTest {
    @Test
    fun testInitialization() {
        val propertyChangedCallback: Observable.OnPropertyChangedCallback = mock()
        val viewModel = TestViewModel()
        viewModel.addOnPropertyChangedCallback(propertyChangedCallback)

        // setting a lazy managed object shouldn't initialize the LazyRelatedModels object
        viewModel.lazyModels.managed = mock()
        assertFalse(viewModel.lazyModels.isInitialized)
        verify(propertyChangedCallback, never()).onPropertyChanged(any(), any())

        // initialize lazyModels by reading something from it
        viewModel.lazyModels.ids
        assertTrue(viewModel.lazyModels.isInitialized)
        verify(propertyChangedCallback, never()).onPropertyChanged(any(), any())

        // updating managed now should trigger a property change
        viewModel.lazyModels.managed = mock()
        verify(propertyChangedCallback).onPropertyChanged(any(), any())
    }

    open class Model : RealmModel, UniqueItem {
        override var id: String? = null
    }

    class TestViewModel : RealmModelViewModel<Model>() {
        val lazyModels = LazyRelatedModels(0) { TestViewModel() }
    }
}
