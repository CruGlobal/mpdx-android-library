package org.mpdx.android.base.databinding

import android.net.Uri
import androidx.databinding.BindingConversion

@BindingConversion
fun String?.toUri() = if (this != null) Uri.parse(this) else null

@BindingConversion
fun Double?.dataBindingToInt() = this?.toInt()
