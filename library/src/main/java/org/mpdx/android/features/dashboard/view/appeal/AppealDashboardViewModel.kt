package org.mpdx.android.features.dashboard.view.appeal

import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.combineWith
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.appeals.realm.getAppeal
import org.mpdx.android.features.appeals.realm.getAppeals
import org.mpdx.android.utils.formatCurrency

@HiltViewModel
class AppealDashboardViewModel @Inject constructor(
    appPrefs: AppPrefs
) : RealmViewModel() {
    private val accountListId = appPrefs.accountListIdLiveData

    private val accountList = accountListId.switchMap {
        realm.getAccountList(it).firstAsLiveData()
    }

    private val primaryAppeal = accountList.switchMap {
        it?.primaryAppealId?.let { appealId ->
            return@switchMap realm.getAppeal(appealId).firstAsLiveData()
        }
        return@switchMap realm.getAppeals(it?.id).firstAsLiveData()
    }

    private val currency = primaryAppeal.map { it?.currency ?: Currency.getInstance(Locale.getDefault()).currencyCode }

    val goalValue = primaryAppeal.map {
        it?.amountInt ?: 0
    }

    val goalCurrency = goalValue.combineWith(currency) { goal, currency ->
        goal.toDouble().formatCurrency(currency)
    }

    val givenValue = primaryAppeal.map { it?.givenInt ?: 0 }

    val givenCurrency = givenValue.combineWith(currency) { given, currency ->
        given.toDouble().formatCurrency(currency)
    }

    private val percentageLeft = givenValue.combineWith(goalValue) { given, goal ->
        return@combineWith given.toDouble() / goal.toDouble()
    }

    val StillNeeded = goalValue.combineWith(givenValue) { goal, given ->
        goal > given
    }

    val stillNeededCurrency = givenValue.combineWith(goalValue, currency) { given, goal, currency ->
        val left = goal - given
        return@combineWith left.toDouble().formatCurrency(currency)
    }

    val appealLabel = givenCurrency.combineWith(percentageLeft) { given, percentage ->
        val formattedPercentage = NumberFormat.getPercentInstance().format(percentage)
        if (percentage > 1) {
            return@combineWith given
        }
        return@combineWith "$given ($formattedPercentage)"
    }

    val appealName = primaryAppeal.map { it?.name }
}
