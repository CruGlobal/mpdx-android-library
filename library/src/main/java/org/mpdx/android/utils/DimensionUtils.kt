package org.mpdx.android.utils

import android.content.Context
import android.util.TypedValue

fun Context.resolveDp(dp: Int) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics)
