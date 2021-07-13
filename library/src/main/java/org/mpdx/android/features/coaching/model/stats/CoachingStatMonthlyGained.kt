package org.mpdx.android.features.coaching.model.stats

import java.util.Currency
import java.util.Locale
import org.mpdx.android.R

class CoachingStatMonthlyGained(private val monthlyGained: Int) : CoachingStatWithCurrency<String?, Int> {
    override val label get() = R.string.coaching_stat_monthly_gained
    override val drawable get() = R.drawable.cru_icon_add_filled_with_yellow
    override val map: Map<String?, Int> get() = mapOf(currency to monthlyGained)

    override var currency: String? = Currency.getInstance(Locale.getDefault()).currencyCode
}
