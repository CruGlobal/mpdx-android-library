package org.mpdx.android.base.databinding

import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.INVALID_POSITION
import android.widget.Spinner
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener

@set:BindingAdapter("value")
@get:InverseBindingAdapter(attribute = "value", event = "valueAttrChanged")
var Spinner.value: Any?
    get() = selectedItem
    set(value) {
        val pos = (0 until count).firstOrNull { value == getItemAtPosition(it) } ?: INVALID_POSITION
        if (pos != selectedItemPosition) setSelection(pos)
    }

@BindingAdapter("valueAttrChanged")
fun Spinner.setValueBindingListener(valueBindingListener: InverseBindingListener?) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) = onChange()
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = onChange()
        private fun onChange() = valueBindingListener?.onChange() ?: Unit
    }
}
