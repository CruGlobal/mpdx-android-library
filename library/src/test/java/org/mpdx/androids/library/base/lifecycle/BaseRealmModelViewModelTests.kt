package org.mpdx.base.lifecycle

import androidx.databinding.Observable
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.realm.RealmModel
import io.realm.kotlin.isManaged
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mpdx.androids.library.base.lifecycle.RealmModelViewModel
import org.mpdx.base.realm.BaseRealmTests
import org.mpdx.utils.copyFromRealm

abstract class BaseRealmModelViewModelTests<T : RealmModel> : BaseRealmTests() {
    internal abstract val viewModel: RealmModelViewModel<T>
    protected lateinit var changeCallback: Observable.OnPropertyChangedCallback

    @Before
    fun setupChangeCallback() {
        changeCallback = mock()
    }

    @Test
    fun testAllowNullModel() {
        viewModel.allowNullModel = true

        viewModel.model = null
        assertNull(viewModel.model)
    }

    @Test
    fun testDisallowNullModel() {
        viewModel.allowNullModel = false

        assertNotNull(viewModel.model)
        viewModel.model = null
        assertNotNull(viewModel.model)
    }

    @Test
    fun testShortCircuitForIdenticalModel() {
        val model = createUnmanagedModel()
        viewModel.model = model
        verify(changeCallback).onPropertyChanged(any(), any())
        reset(changeCallback)

        viewModel.model = model
        verify(changeCallback, never()).onPropertyChanged(any(), any())
    }

    @Test
    fun testForceUnmanaged() {
        val managed = createManagedModel()
        val unmanaged = createUnmanagedModel()
        viewModel.forceUnmanaged = true
        whenever(managed.copyFromRealm()).thenReturn(unmanaged)

        viewModel.model = managed
        assertFalse(viewModel.model!!.isManaged())
    }

    internal abstract fun createUnmanagedModel(): T
    internal abstract fun createManagedModel(): T
}
