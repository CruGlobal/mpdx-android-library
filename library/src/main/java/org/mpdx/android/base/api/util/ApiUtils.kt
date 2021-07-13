package org.mpdx.android.base.api.util

import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.mpdx.android.utils.OpenRange
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

fun JsonApiParams.filter(type: String, value: String) = apply { put("filter[$type]", value) }
fun JsonApiParams.page(page: Int) = apply { put("page", page.toString()) }
fun JsonApiParams.perPage(limit: Int) = apply { put("per_page", limit.toString()) }

fun ClosedRange<LocalDate>.asApiDateRange() = "$start..$endInclusive"
fun ClosedRange<Instant>.asApiTimeRange() = "$start..$endInclusive"
fun OpenRange<LocalDate>.asApiDateRange() = "$start...$endExclusive"
fun OpenRange<Instant>.asApiTimeRange() = "$start...$endExclusive"
