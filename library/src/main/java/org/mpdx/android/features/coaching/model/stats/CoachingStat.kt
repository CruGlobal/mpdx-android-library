package org.mpdx.android.features.coaching.model.stats

interface CoachingStat<T, U> {
    val map: Map<T, U>
    val label: Int
    val drawable: Int
}
