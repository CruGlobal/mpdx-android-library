package org.mpdx.android.utils

import java.util.Date
import java.util.Locale
import org.threeten.bp.DateTimeException
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.MonthDay
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset.UTC
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import org.threeten.bp.temporal.ChronoField
import org.threeten.bp.temporal.WeekFields

fun monthDayOrNull(month: Int?, day: Int?) = try {
    if (month != null && day != null) MonthDay.of(month, day) else null
} catch (e: DateTimeException) {
    null
}

fun String.toMonthDayOrNull(formatter: DateTimeFormatter? = null) =
    try {
        if (formatter != null) MonthDay.parse(this, formatter) else MonthDay.parse(this)
    } catch (e: DateTimeParseException) {
        null
    }

// region Instant
fun Date.toBpInstant(): Instant = DateTimeUtils.toInstant(this)
fun String.toInstanOrNull() =
    try {
        Instant.parse(this)
    } catch (e: DateTimeParseException) {
        null
    }

fun Instant.withoutFractionalSeconds(): Instant = with(ChronoField.NANO_OF_SECOND, 0)

fun Instant.toDate(): Date = DateTimeUtils.toDate(this)
// endregion Instant

val MonthDay.dayOfYear get() = atYear(2000).dayOfYear

fun monthDayComparator(startAt: MonthDay = MonthDay.of(1, 1)): Comparator<MonthDay?> =
    nullsLast(compareBy { day -> day.dayOfYear.let { if (it < startAt.dayOfYear) it + 366 else it } })

// region LocalDate
fun localDateOrNull(year: Int?, month: Int?, day: Int?) = try {
    if (year != null && month != null && day != null) LocalDate.of(year, month, day) else null
} catch (e: DateTimeException) {
    null
}

fun Date.toLocalDate(): LocalDate = toBpInstant().atZone(UTC).toLocalDate()
fun String.toLocalDateOrNull(formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE) =
    try {
        LocalDate.parse(this, formatter)
    } catch (e: DateTimeParseException) {
        null
    }

fun LocalDate.toDate() = atStartOfDay(UTC).toDate()
fun LocalDate.toMonthDay(): MonthDay = MonthDay.from(this)
fun LocalDate.toContainingWeekRange(locale: Locale = Locale.getDefault()): ClosedRange<LocalDate> {
    val startOfWeek = WeekFields.of(locale).dayOfWeek().adjustInto(this, 1)
    return startOfWeek.rangeTo(startOfWeek.plusDays(6))
}

fun ClosedRange<LocalDate>.minusWeeks(weeksToSubtract: Long) =
    start.minusWeeks(weeksToSubtract)..endInclusive.minusWeeks(weeksToSubtract)
// endregion LocalDate

// region YearMonth
fun YearMonth.toLocalDateRange() = atDay(1)..atEndOfMonth()

val ClosedRange<YearMonth>.size: Int
    get() = (endInclusive.getLong(ChronoField.PROLEPTIC_MONTH) - start.getLong(ChronoField.PROLEPTIC_MONTH) + 1).toInt()
operator fun ClosedRange<YearMonth>.get(index: Int): YearMonth = start.plusMonths(index.toLong())
fun ClosedRange<YearMonth>.indexOf(month: YearMonth): Int = when {
    month < start -> -1
    month > endInclusive -> -1
    else -> (month.getLong(ChronoField.PROLEPTIC_MONTH) - start.getLong(ChronoField.PROLEPTIC_MONTH)).toInt()
}
// endregion YearMonth

// region ZonedDateTime
fun Date.toZonedDateTime(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime = toBpInstant().atZone(zone)
fun LocalDate.toZonedDateTime(time: LocalTime? = null, zone: ZoneId = ZoneId.systemDefault()) =
    ZonedDateTime.of(this, time ?: LocalTime.MIDNIGHT, zone)
fun ZonedDateTime.toDate() = toInstant().toDate()
// endregion ZonedDateTime
