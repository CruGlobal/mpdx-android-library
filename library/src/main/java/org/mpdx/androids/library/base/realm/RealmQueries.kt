package org.mpdx.androids.library.base.realm

import io.realm.Case
import io.realm.RealmModel
import io.realm.RealmQuery
import org.mpdx.utils.dayOfYear
import org.mpdx.utils.toDate
import org.threeten.bp.LocalDate
import org.threeten.bp.MonthDay
import org.threeten.bp.ZoneOffset.UTC
import org.threeten.bp.ZonedDateTime

// region LocalDate comparisons
fun <T : RealmModel> RealmQuery<T>.between(fieldName: String, from: LocalDate, to: LocalDate): RealmQuery<T> {
    val fromDate = from.atStartOfDay(UTC).toDate()
    val toDate = to.plusDays(1).atStartOfDay(UTC).toInstant().minusMillis(1).toDate()
    return between(fieldName, fromDate, toDate)
}

fun <T : RealmModel> RealmQuery<T>.lessThan(fieldName: String, value: LocalDate): RealmQuery<T> =
    lessThan(fieldName, value.toDate())
fun <T : RealmModel> RealmQuery<T>.lessThanOrEqualTo(fieldName: String, value: LocalDate) =
    lessThan(fieldName, value.plusDays(1))
fun <T : RealmModel> RealmQuery<T>.greaterThan(fieldName: String, value: LocalDate) =
    greaterThanOrEqualTo(fieldName, value.plusDays(1))
fun <T : RealmModel> RealmQuery<T>.greaterThanOrEqualTo(fieldName: String, value: LocalDate): RealmQuery<T> =
    greaterThanOrEqualTo(fieldName, value.toDate())
// endregion LocalDate comparisons

fun <T : RealmModel> RealmQuery<T>.between(fieldName: String, from: ZonedDateTime, to: ZonedDateTime): RealmQuery<T> =
    between(fieldName, from.toDate(), to.toDate())

fun <T : RealmModel> RealmQuery<T>.between(fieldName: String, from: MonthDay, to: MonthDay) =
    betweenWrapping(fieldName, from.dayOfYear, to.dayOfYear)

fun <T : RealmModel> RealmQuery<T>.betweenWrapping(fieldName: String, from: Int, to: Int): RealmQuery<T> =
    if (from <= to) between(fieldName, from, to)
    else beginGroup().greaterThanOrEqualTo(fieldName, from).or().lessThanOrEqualTo(fieldName, to).endGroup()

fun <E> RealmQuery<E>.filterByQuery(fieldName: String, query: String?): RealmQuery<E> =
    if (!query.isNullOrEmpty()) contains(fieldName, query, Case.INSENSITIVE) else this
