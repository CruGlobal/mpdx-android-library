package org.mpdx.android.features.dashboard.view.monthly_activity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import org.ccci.gto.android.common.androidx.lifecycle.combineWith
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.model.CombineChartLabelValueData
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.donations.realm.forAccountList
import org.mpdx.android.features.donations.realm.forMonth
import org.mpdx.android.features.donations.realm.getDonations
import org.mpdx.android.utils.CurrencyFormatter
import org.threeten.bp.YearMonth
import org.threeten.bp.format.TextStyle

@HiltViewModel
class MonthlyActivityViewModel @Inject constructor(
    appPrefs: AppPrefs,
    currencyFormatter: CurrencyFormatter
) : RealmViewModel() {
    private val accountListId = appPrefs.accountListIdLiveData

    // region Current Data Tracker
    private val currentYearMonth = MutableLiveData(YearMonth.now())
    private val currentYearMonthDonationTotal =
        accountListId.distinctUntilChanged().switchCombineWith(currentYearMonth) { id, yearMonth ->
            realm.getDonations().forAccountList(id).forMonth(yearMonth).asLiveData()
        }
    // endregion Current Data Tracker

    private val accountList = accountListId.switchMap { realm.getAccountList(it).firstAsLiveData() }
    private val goal = accountList.map { it?.monthlyGoal }
    val goalCurrency = accountList.combineWith(goal) { accountList, goal ->
        currencyFormatter.formatForCurrency(goal ?: 0.0, accountList?.currency)
    }
    private val commitment = accountList.map { it?.committed }
    val barEntries =
        accountListId.distinctUntilChanged()
            .combineWith(currentYearMonthDonationTotal, commitment) { accountListId, _, commitment ->
                val entries = arrayListOf<CombineChartLabelValueData>()

                for (i in 0..11) {
                    var iMonthYear = YearMonth.now()
                    if (i > 0) {
                        iMonthYear = iMonthYear.plusMonths(-i.toLong())
                    }

                    var donationTotal = 0.0
                    realm.getDonations().forAccountList(accountListId).forMonth(iMonthYear).findAll().forEach {
                        val amount = it.convertedAmount ?: 0.0
                        donationTotal += amount
                    }

                    val monthLabel = iMonthYear.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val data = CombineChartLabelValueData()
                    data.label = monthLabel
                    data.value = donationTotal.roundToInt().toFloat()
                    data.committed = commitment?.toFloat() ?: 0f
                    entries.add(i, data)
                }
                return@combineWith entries
            }
}
