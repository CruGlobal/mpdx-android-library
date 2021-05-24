package org.mpdx.androids.library.base.databinding

import android.annotation.SuppressLint
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.graphics.Typeface
import android.widget.TextView
import androidx.databinding.BindingAdapter

@SuppressLint("WrongConstant")
@BindingAdapter("boldIf")
fun TextView.boldIf(bold: Boolean) {
    val style = typeface?.style ?: Typeface.NORMAL
    val newStyle = if (bold) style or Typeface.BOLD else style and Typeface.BOLD.inv()
    typeface = Typeface.create(typeface, newStyle)
}

@BindingAdapter("strikethroughIf")
fun TextView.strikethroughIf(strikethrough: Boolean) {
    paintFlags = if (strikethrough) paintFlags or STRIKE_THRU_TEXT_FLAG else paintFlags and STRIKE_THRU_TEXT_FLAG.inv()
}
