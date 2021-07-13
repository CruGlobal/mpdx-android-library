package org.mpdx.android.features.coaching.model.stats

import java.util.Currency
import java.util.Locale
import org.mpdx.android.R

class CoachingStatMonthlyLost(private val monthlyLost: Int) : CoachingStatWithCurrency<String?, Int> {
    override val label get() = R.string.coaching_stat_monthly_lost
    override val drawable get() = R.drawable.cru_icon_monthly_lost
    override val map get() = mapOf(currency to monthlyLost)

    override var currency: String? = Currency.getInstance(Locale.getDefault()).currencyCode
}
