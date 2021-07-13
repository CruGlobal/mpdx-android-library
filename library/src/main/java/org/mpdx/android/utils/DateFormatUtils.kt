package org.mpdx.android.utils

import android.annotation.SuppressLint
import android.icu.text.DateFormat.ABBR_MONTH_DAY
import android.icu.text.DateFormat.MONTH_DAY
import android.icu.text.DateFormat.NUM_MONTH_DAY
import android.icu.text.DateFormat.YEAR_ABBR_MONTH
import android.icu.text.DateFormat.YEAR_MONTH
import android.icu.text.DateFormat.YEAR_NUM_MONTH
import android.text.format.DateFormat
import java.util.Locale
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

fun localizedMonthDayFormatter(
    style: FormatStyle = FormatStyle.LONG,
    locale: Locale = Locale.getDefault()
): DateTimeFormatter {
    @SuppressLint("InlinedApi")
    val skeleton = when (style) {
        FormatStyle.LONG, FormatStyle.FULL -> MONTH_DAY
        FormatStyle.MEDIUM -> ABBR_MONTH_DAY
        FormatStyle.SHORT -> NUM_MONTH_DAY
    }
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

fun localizedYearMonthFormatter(
    style: FormatStyle = FormatStyle.LONG,
    locale: Locale = Locale.getDefault()
): DateTimeFormatter {
    @SuppressLint("InlinedApi")
    val skeleton = when (style) {
        FormatStyle.LONG, FormatStyle.FULL -> YEAR_MONTH
        FormatStyle.MEDIUM -> YEAR_ABBR_MONTH
        FormatStyle.SHORT -> YEAR_NUM_MONTH
    }
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

inline fun localizedDateFormatter(
    style: FormatStyle = FormatStyle.LONG,
    locale: Locale = Locale.getDefault()
) = localizedDateTimeFormatter(style = null, dateStyle = style, locale = locale)

inline fun localizedTimeFormatter(
    style: FormatStyle = FormatStyle.LONG,
    locale: Locale = Locale.getDefault()
) = localizedDateTimeFormatter(style = null, timeStyle = style, locale = locale)

fun localizedDateTimeFormatter(
    style: FormatStyle? = FormatStyle.LONG,
    dateStyle: FormatStyle? = style,
    timeStyle: FormatStyle? = style,
    locale: Locale = Locale.getDefault()
): DateTimeFormatter = when {
    dateStyle != null && timeStyle != null -> DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle)
    dateStyle != null && timeStyle == null -> DateTimeFormatter.ofLocalizedDate(dateStyle)
    dateStyle == null && timeStyle != null -> DateTimeFormatter.ofLocalizedTime(timeStyle)
    else -> throw IllegalArgumentException("a date or time FormatStyle must be specified")
}.withLocale(locale)
