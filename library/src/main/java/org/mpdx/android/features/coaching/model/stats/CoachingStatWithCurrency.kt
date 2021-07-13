package org.mpdx.android.features.coaching.model.stats

interface CoachingStatWithCurrency<T, U> : CoachingStat<T, U> {
    var currency: String?
}
