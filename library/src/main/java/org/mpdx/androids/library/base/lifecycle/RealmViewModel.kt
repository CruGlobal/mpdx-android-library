package org.mpdx.androids.library.base.lifecycle

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import io.realm.Realm

abstract class RealmViewModel : ViewModel() {
    @get:MainThread
    protected val realm: Realm = Realm.getDefaultInstance()

    @CallSuper
    @MainThread
    override fun onCleared() {
        super.onCleared()
        realm.close()
    }
}
