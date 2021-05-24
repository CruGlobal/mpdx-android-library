package org.mpdx.androids.library.base.realm

import androidx.lifecycle.LiveData
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults

fun <T : RealmModel> RealmQuery<T>.asLiveData(): LiveData<RealmResults<T>> = RealmListLiveData(findAllAsync())
fun <T : RealmModel> RealmQuery<T>.firstAsLiveData(): LiveData<T?> = RealmLiveData(findAllAsync())
fun <T : RealmModel> RealmResults<T>.asLiveData(): LiveData<RealmResults<T>> = RealmListLiveData(this)
fun <T : RealmModel> RealmResults<T>.firstAsLiveData(): LiveData<T?> = RealmLiveData(this)

private class RealmLiveData<T : RealmModel>(private val results: RealmResults<T>) : LiveData<T?>(null) {
    private val listener: RealmChangeListener<RealmResults<T>> = RealmChangeListener { value = results.first(null) }

    override fun onActive() {
        results.addChangeListener(listener)
        value = results.first(null)
    }

    override fun onInactive() = results.removeChangeListener(listener)
}

private class RealmListLiveData<T : RealmModel>(private val results: RealmResults<T>) :
    LiveData<RealmResults<T>>(results) {
    private val listener: RealmChangeListener<RealmResults<T>> = RealmChangeListener { value = results }

    override fun onActive() {
        results.addChangeListener(listener)
        value = results
    }

    override fun onInactive() = results.removeChangeListener(listener)
}
