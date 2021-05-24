package org.mpdx.androids.library.base.lifecycle

import androidx.annotation.VisibleForTesting
import androidx.databinding.Observable
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.realm.RealmModel
import io.realm.RealmResults
import io.realm.kotlin.isManaged
import org.mpdx.BR
import org.mpdx.androids.library.base.model.UniqueItem
import org.mpdx.androids.library.base.model.addUnique
import org.mpdx.utils.copyFromRealm
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

abstract class RealmModelViewModel<T : RealmModel> : ObservableViewModel(), Observer<T?> {
    var allowNullModel = true
    var forceUnmanaged = false

    var model: T? = null
        get() = field ?: if (!allowNullModel) createModel().also { model = it } else null
        set(value) {
            // short-circuit if the model isn't actually being changed
            val model = when {
                forceUnmanaged && value?.isManaged() == true -> value.copyFromRealm()
                value != null -> value
                !allowNullModel -> createModel()
                else -> null
            }
            if (field === model) return

            // update the actual field
            updatingModel(field, model)
            field = model
            notifyChange()

            // update any related fields
            updateRelated(value)
        }

    protected open fun updatingModel(old: T?, new: T?) = Unit

    protected open fun createModel(): T =
        throw UnsupportedOperationException("createModel() was not implemented for this ViewModel")
    protected open fun updateRelated(model: T?) = Unit

    // region Observer
    override fun onChanged(t: T?) {
        model = t
    }
    // endregion Observer

    // region Model Properties
    protected fun <P> modelProperty(
        fieldId: Int = BR._all,
        valueIfNull: P,
        isEqual: (P, P) -> Boolean = { x, y -> x == y },
        onSet: ((P) -> Unit)? = null,
        propertyRef: (T) -> KMutableProperty0<P>
    ) = ModelPropertyDelegate(fieldId, valueIfNull, isEqual, onSet, propertyRef)
        as ReadWriteProperty<RealmModelViewModel<T>, P>

    protected inline fun <P> modelNullableProperty(
        fieldId: Int = BR._all,
        noinline isEqual: (P?, P?) -> Boolean = { x, y -> x == y },
        noinline onSet: ((P?) -> Unit)? = null,
        noinline propertyRef: (T) -> KMutableProperty0<P?>
    ) = modelProperty(fieldId, null, isEqual, onSet, propertyRef)

    protected inline fun modelBooleanProperty(
        fieldId: Int = BR._all,
        valueIfNull: Boolean = false,
        noinline onSet: ((Boolean) -> Unit)? = null,
        noinline propertyRef: (T) -> KMutableProperty0<Boolean>
    ) = modelProperty(fieldId, valueIfNull, onSet = onSet, propertyRef = propertyRef)

    protected inline fun modelNullableBooleanProperty(
        fieldId: Int = BR._all,
        valueIfNull: Boolean? = false,
        noinline isEqual: (Boolean?, Boolean?) -> Boolean = { x, y -> x == y },
        noinline onSet: ((Boolean?) -> Unit)? = null,
        noinline propertyRef: (T) -> KMutableProperty0<Boolean?>
    ) = modelProperty(fieldId, valueIfNull, isEqual, onSet, propertyRef)

    protected fun modelStringProperty(
        fieldId: Int = BR._all,
        onSet: ((String?) -> Unit)? = null,
        propertyRef: (T) -> KMutableProperty0<String?>
    ) = modelProperty(fieldId, null, { x, y -> x.orEmpty() == y.orEmpty() }, onSet, propertyRef)

    private inner class ModelPropertyDelegate<P>(
        private val fieldId: Int = BR._all,
        private val valueIfNull: P,
        private val isEqual: (P, P) -> Boolean,
        private val onSet: ((P) -> Unit)?,
        private val propertyRef: (T) -> KMutableProperty0<P>
    ) : ReadWriteProperty<RealmModelViewModel<T>, P> {
        override fun getValue(thisRef: RealmModelViewModel<T>, property: KProperty<*>): P =
            model?.let { propertyRef(it) }?.get() ?: valueIfNull

        override fun setValue(thisRef: RealmModelViewModel<T>, property: KProperty<*>, value: P) {
            model?.let { propertyRef(it) }?.apply {
                if (!isEqual(value ?: valueIfNull, get())) {
                    set(value ?: valueIfNull)
                    onSet?.invoke(value ?: valueIfNull)
                    notifyPropertyChanged(fieldId)
                }
            }
        }
    }
    // endregion Model Properties

    // region Related Models
    protected class LazyViewModel<T : RealmModel, VM : RealmModelViewModel<T>>(private val creator: () -> VM) {
        var model: T? = null
            set(value) {
                field = value
                if (viewModelDelegate.isInitialized()) viewModel.model = value
            }

        private val viewModelDelegate = lazy { creator().also { it.model = model } }
        val viewModel by viewModelDelegate
    }

    inner class LazyRelatedModels<E, VM : RealmModelViewModel<E>>(
        private val fieldId: Int,
        private val viewModelCreator: (() -> VM)
    ) where E : UniqueItem, E : RealmModel {
        var managed: LiveData<RealmResults<E>>? = null
            set(value) {
                field = value
                if (relatedModelsDelegate.isInitialized()) relatedModels.managed = value
            }

        val isInitialized get() = relatedModelsDelegate.isInitialized()
        fun initialize() = relatedModels

        private val relatedModelsDelegate = lazy { RelatedModels(fieldId, managed, viewModelCreator) }
        private val relatedModels by relatedModelsDelegate

        val models get() = relatedModels.models
        val ids get() = relatedModels.ids
        val viewModels get() = relatedModels.viewModels
        val deleted get() = if (isInitialized) relatedModels.deleted else emptySet()
        val size get() = relatedModels.size

        fun addModel(model: E): VM? = relatedModels.addModel(model)
        fun deleteModel(id: String?) = relatedModels.deleteModel(id)
    }

    inner class RelatedModels<E, VM : RealmModelViewModel<E>>(
        private val fieldId: Int,
        initialManaged: LiveData<RealmResults<E>>? = null,
        private val viewModelCreator: (() -> VM)
    ) where E : UniqueItem, E : RealmModel {
        @VisibleForTesting
        internal var existing: List<E>? = null
            set(value) {
                field = value
                notifyPropertyChanged(fieldId)
            }
        private val newModels = mutableListOf<E>()
        val deleted = mutableSetOf<String>()
        val models get() = (existing.orEmpty() + newModels).distinctBy { it.id }.filter { !deleted.contains(it.id) }

        val ids: Set<String> get() = models.mapNotNullTo(mutableSetOf()) { it.id }
        val size get() = models.size
        fun addModel(model: E): VM? {
            if (model.id == null) return null
            newModels.addUnique(model)
            model.id?.let { deleted.remove(it) }
            val viewModel = viewModelFor(model)
            viewModel?.model = model
            notifyPropertyChanged(fieldId)
            return viewModel
        }

        fun deleteModel(id: String?) = id?.let {
            newModels.removeAll { it.id == id }
            deleted.add(id)
            notifyPropertyChanged(fieldId)
        }

        val viewModels: List<VM> get() = models.mapNotNull { viewModelFor(it) }
        private val cachedViewModels = mutableMapOf<String, VM>()
        private fun viewModelFor(item: E) = item.id?.let { id ->
            cachedViewModels.getOrPut(id) {
                viewModelCreator().also {
                    it.model = item
                    it.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
                        override fun onPropertyChanged(sender: Observable?, propertyId: Int) =
                            notifyPropertyChanged(fieldId)
                    })
                }
            }
        }

        private val managedObserver = Observer<RealmResults<E>> { data ->
            data?.forEach { viewModelFor(it)?.model = it }
            existing = if (data?.isManaged == true) data.copyFromRealm() else data
        }
        var managed: LiveData<RealmResults<E>>? = initialManaged?.also { it.observeForever(managedObserver) }
            set(value) {
                if (field === value) return
                field?.removeObserver(managedObserver)
                field = value
                field?.observeForever(managedObserver)
                notifyPropertyChanged(fieldId)
            }
    }
    // endregion Related Models

    // region Validation
    open fun hasErrors() = false
    open fun showErrors() = Unit
    // endregion Validation
}
