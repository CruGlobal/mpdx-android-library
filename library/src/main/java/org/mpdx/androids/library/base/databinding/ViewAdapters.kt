package org.mpdx.androids.library.base.databinding

import android.view.View
import androidx.databinding.BindingAdapter

/**
 * This binding adapter allows us to work around a data binding generation bug.
 * If an observable field is only accessed from a listener binding, the flags for that field are not correctly generated
 * and it is unable to generate the listener binding.
 *
 * TODO: extract a simple test case and report to Google.
 */
@BindingAdapter("dataBindingFlagsHack")
fun View.dataBindingFlagHack(obj: Any?) = Unit
