package org.mpdx.android.base.lifecycle

import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.ViewModel

abstract class ObservableViewModel : ViewModel(), Observable {
    // region Observable
    @Transient
    private lateinit var callbacks: PropertyChangeRegistry

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        synchronized(this) {
            if (!::callbacks.isInitialized) callbacks = PropertyChangeRegistry()
        }
        callbacks.add(callback)
    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        synchronized(this) { if (!::callbacks.isInitialized) return }
        callbacks.remove(callback)
    }

    protected fun notifyChange() {
        synchronized(this) { if (!::callbacks.isInitialized) return }
        callbacks.notifyCallbacks(this, 0, null)
    }

    protected fun notifyPropertyChanged(fieldId: Int) {
        synchronized(this) { if (!::callbacks.isInitialized) return }
        callbacks.notifyCallbacks(this, fieldId, null)
    }
    // endregion Observable
}
