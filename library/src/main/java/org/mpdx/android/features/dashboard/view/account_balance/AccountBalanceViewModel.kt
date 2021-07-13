package org.mpdx.android.features.dashboard.view.account_balance

import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.combineWith
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.dashboard.realm.forAccountList
import org.mpdx.android.features.dashboard.realm.getGoalProgress
import org.mpdx.android.utils.formatCurrency

@HiltViewModel
class AccountBalanceViewModel @Inject constructor(appPrefs: AppPrefs) : RealmViewModel() {
    private val accountListId = appPrefs.accountListIdLiveData
    private val goalProgress = accountListId.switchMap { realm.getGoalProgress().forAccountList(it).firstAsLiveData() }
    private val balance = goalProgress.map { it?.salaryBalance }
    private val currency = goalProgress.map {
        it?.salaryCurrencyOrDefault ?: Currency.getInstance(Locale.getDefault()).currencyCode
    }

    val balanceCurrency = balance.combineWith(currency) { balance, currency ->
        (balance ?: 0.0).formatCurrency(currency)
    }
}
