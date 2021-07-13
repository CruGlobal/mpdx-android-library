package org.mpdx.android.utils

import androidx.lifecycle.MutableLiveData

operator fun <T> MutableLiveData<out MutableCollection<T>>.plusAssign(values: Collection<T>) {
    val current = value ?: mutableSetOf()
    current.addAll(values)
    value = current
}

operator fun <T> MutableLiveData<out MutableCollection<T>>.minusAssign(values: Collection<T>) {
    val current = value ?: mutableSetOf()
    current.removeAll(values)
    value = current
}
