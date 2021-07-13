package org.mpdx.android.base.databinding

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.adapters.TextViewBindingAdapter
import org.mpdx.android.utils.localizedDateFormatter
import org.mpdx.android.utils.localizedDateTimeFormatter
import org.mpdx.android.utils.localizedMonthDayFormatter
import org.mpdx.android.utils.localizedTimeFormatter
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.MonthDay
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.FormatStyle

// region LocalDate BindingAdapters
@BindingAdapter("android:text")
fun TextView.bindLocalDate(localDate: LocalDate?) = bindLocalDate(localDate, FormatStyle.LONG)

@BindingAdapter("android:text", "dateStyle")
fun TextView.bindLocalDate(localDate: LocalDate?, style: FormatStyle) = bindLocalDate(localDate, style, null)

@SuppressLint("RestrictedApi")
@BindingAdapter("android:text", "dateStyle", "defaultText")
fun TextView.bindLocalDate(localDate: LocalDate?, style: FormatStyle, defaultText: String?) {
    TextViewBindingAdapter.setText(this, localDate?.format(localizedDateFormatter(style)) ?: defaultText)
}
// endregion LocalDate BindingAdapters

// region LocalTime BindingAdapters
@BindingAdapter("android:text")
fun TextView.bindLocalTime(localTime: LocalTime?) = bindLocalTime(localTime, FormatStyle.LONG)

@BindingAdapter("android:text", "timeStyle")
fun TextView.bindLocalTime(localTime: LocalTime?, style: FormatStyle) = bindLocalTime(localTime, style, null)

@SuppressLint("RestrictedApi")
@BindingAdapter("android:text", "timeStyle", "defaultText")
fun TextView.bindLocalTime(localTime: LocalTime?, style: FormatStyle, defaultText: String?) {
    TextViewBindingAdapter.setText(this, localTime?.format(localizedTimeFormatter(style)) ?: defaultText)
}
// endregion LocalTime BindingAdapters

// region MonthDay BindingAdapters
@BindingAdapter("android:text")
fun TextView.bindMonthDay(monthDay: MonthDay?) = bindMonthDay(monthDay, FormatStyle.LONG)

@SuppressLint("RestrictedApi")
@BindingAdapter("android:text", "dateStyle")
fun TextView.bindMonthDay(monthDay: MonthDay?, style: FormatStyle) {
    TextViewBindingAdapter.setText(this, monthDay?.format(localizedMonthDayFormatter(style)))
}
// endregion MonthDay BindingAdapters

// region ZonedDateTime BindingAdapters
@SuppressLint("RestrictedApi")
@BindingAdapter("android:text")
fun TextView.bindZonedDateTime(time: ZonedDateTime?) = bindZonedDateTime(time, FormatStyle.LONG)

@SuppressLint("RestrictedApi")
@BindingAdapter("android:text", "dateTimeStyle", "dateStyle", "timeStyle", requireAll = false)
fun TextView.bindZonedDateTime(
    time: ZonedDateTime?,
    dateTimeStyle: FormatStyle?,
    dateStyle: FormatStyle? = null,
    timeStyle: FormatStyle? = null
) = TextViewBindingAdapter.setText(
    this, time?.format(localizedDateTimeFormatter(null, dateStyle ?: dateTimeStyle, timeStyle ?: dateTimeStyle))
)
// endregion ZonedDateTime BindingAdapters
