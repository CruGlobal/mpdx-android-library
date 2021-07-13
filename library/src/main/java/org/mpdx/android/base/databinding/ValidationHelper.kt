package org.mpdx.android.base.databinding

import android.view.View
import androidx.databinding.BaseObservable
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout
import com.karumi.weak.weak
import org.mpdx.android.R

class ValidationHelper(private val spec: () -> Int?) : BaseObservable() {
    internal val error get() = spec()
    internal val hasError get() = error != null

    internal var showErrors = false
        get() = field || wasFocused
        set(value) {
            if (field == value) return
            field = value
            notifyChange()
        }

    internal var isFocused = false
        set(value) {
            if (field == value) return
            wasFocused = wasFocused || field
            field = value
        }
    private var wasFocused = false
        set(value) {
            if (field == value) return
            field = value
            notifyChange()
        }
}

@BindingAdapter("validation")
fun TextInputLayout.attachValidation(validation: ValidationHelper?) {
    editText?.validationFocusListener?.helper = validation

    // show actual error message
    if (validation?.showErrors != true) {
        isErrorEnabled = false
        return
    }
    val errorMsg = validation.error
    if (errorMsg != null) error = context.getText(errorMsg) else isErrorEnabled = false
}

private val View.validationFocusListener: ValidationFocusListener
    get() = getTag(R.id.validationFocusListener) as? ValidationFocusListener
        ?: ValidationFocusListener(this).also { setTag(R.id.validationFocusListener, it) }

private class ValidationFocusListener(view: View) : View.OnFocusChangeListener {
    init {
        view.onFocusChangeListener = this
    }

    var helper: ValidationHelper? by weak()

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        helper?.isFocused = hasFocus
    }
}
