@file:BindingMethods(
    BindingMethod(
        type = TextInputLayout::class,
        attribute = "endIconOnClick",
        method = "setEndIconOnClickListener"
    )
)

package org.mpdx.android.base.databinding

import androidx.databinding.BindingMethod
import androidx.databinding.BindingMethods
import com.google.android.material.textfield.TextInputLayout

// HACK: we need to include some actual code so that the BindingMethods annotation actually gets applied to a file
private const val HACK = true
