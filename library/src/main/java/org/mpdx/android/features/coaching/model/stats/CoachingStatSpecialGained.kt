package org.mpdx.android.features.coaching.model.stats

import java.util.Currency
import java.util.Locale
import org.mpdx.android.R

open class CoachingStatSpecialGained(private val specialGained: Int) : CoachingStatWithCurrency<String?, Int> {
    override val label get() = R.string.coaching_stat_special_gained
    override val drawable get() = R.drawable.cru_icon_special_gained
    override val map get() = mapOf(currency to specialGained)

    override var currency = Currency.getInstance(Locale.getDefault()).currencyCode
}
