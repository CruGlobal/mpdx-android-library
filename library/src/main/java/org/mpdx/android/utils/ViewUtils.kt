package org.mpdx.android.utils

import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Spinner
import androidx.core.content.getSystemService

fun Spinner.getCurrentSelectionAsString() = getItemAtPosition(selectedItemPosition)?.toString()

fun View.resolveDp(dp: Int) = context.resolveDp(dp)

fun View.showKeyboard() = context.getSystemService<InputMethodManager>()
    ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)

fun View.hideKeyboard() = context.getSystemService<InputMethodManager>()
    ?.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
