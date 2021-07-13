package org.mpdx.android.base.lifecycle

import io.realm.RealmModel
import io.realm.kotlin.isManaged
import org.ccci.gto.android.common.androidx.lifecycle.databinding.getPropertyLiveData
import org.mpdx.android.base.model.ChangeAwareItem

abstract class ChangeAwareViewModel<T> : RealmModelViewModel<T>() where T : RealmModel, T : ChangeAwareItem {
    var trackingChanges = false

    final override fun updatingModel(old: T?, new: T?) {
        // transfer any tracked changes to the new model object
        if (new?.isManaged() == false) new.changedFieldsStr = ""
        if (old != null && trackingChanges) {
            old.trackingChanges = false
            new?.mergeChangedFields(old)
        }
        new?.trackingChanges = trackingChanges
    }

    open val hasChanges: Boolean get() = model?.hasChangedFields == true
    val hasChangesLiveData by lazy { getPropertyLiveData(::hasChanges) }
    fun mergeChangesFrom(source: T) {
        model?.mergeChangedFields(source)
        notifyChange()
    }
}
