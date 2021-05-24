package org.mpdx.androids.library.base.databinding

import android.view.View
import android.widget.CompoundButton
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingListener
import org.mpdx.R

private var View.isBinding
    get() = getTag(R.id.isBinding) == true
    set(value) = setTag(R.id.isBinding, value)

@BindingAdapter("android:checked")
internal fun CompoundButton.setCheckedBindingAware(checked: Boolean) {
    if (isChecked != checked) {
        isBinding = true
        isChecked = checked
        isBinding = false
    }
}

@BindingAdapter("android:onCheckedChanged", "suppressListenerWhileBinding", "android:checkedAttrChanged")
internal fun CompoundButton.setListeners(
    listener: CompoundButton.OnCheckedChangeListener?,
    suppressListenerWhileBinding: Boolean,
    attrChange: InverseBindingListener?
) {
    setOnCheckedChangeListener { buttonView, isChecked ->
        if (!(suppressListenerWhileBinding && isBinding)) listener?.onCheckedChanged(buttonView, isChecked)
        attrChange?.onChange()
    }
}
