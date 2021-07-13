package org.mpdx.android.features.dashboard.view.monthly_goal

import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.math.roundToInt
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.dashboard.realm.forAccountList
import org.mpdx.android.features.dashboard.realm.getGoalProgress
import org.mpdx.android.utils.formatCurrency

@HiltViewModel
class MonthlyGoalViewModel @Inject constructor(
    appPrefs: AppPrefs
) : RealmViewModel() {

    private val accountListId = appPrefs.accountListIdLiveData

    private val model = accountListId.switchMap { realm.getGoalProgress().forAccountList(it).firstAsLiveData() }

    val monthlyGoal = model.map { it?.monthlyGoal?.roundToInt() }
    private val salaryCurrency = model.map { it?.salaryCurrencyOrDefault }
    val monthlyGoalCurrency = model.map { model ->
        model?.monthlyGoal?.formatCurrency(model.salaryCurrencyOrDefault)
    }

    val received = model.map { it?.receivedPledges?.roundToInt() }
    val monthlyGiftStartedCurrency = model.map { goal ->
        val startedValueString = goal?.receivedPledges?.formatCurrency(goal.salaryCurrencyOrDefault)
        return@map "$startedValueString (${goal?.inHandPercent ?: 0.0}%)"
    }
    val pledged = model.map { it?.totalPledges?.roundToInt() }
    val monthlyCommitmentCurrency = model.map { model ->
        val startedValueString = model?.totalPledges?.formatCurrency(model.salaryCurrencyOrDefault)
        return@map "$startedValueString (${model?.pledgedPercent ?: 0.0}%)"
    }

    val monthlyBelowGoalCurrency = model.map { goalProgress ->
        val goal = goalProgress?.monthlyGoal ?: 0.0
        val committed = goalProgress?.totalPledges ?: 0.0
        val below = goal - committed
        val belowPercentage = NumberFormat.getPercentInstance().format(below / goal)
        val belowCurrency = below.formatCurrency(goalProgress?.salaryCurrencyOrDefault)
        return@map "$belowCurrency ($belowPercentage)"
    }
}
