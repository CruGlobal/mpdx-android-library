package org.mpdx.android.features.coaching.viewmodel

import androidx.databinding.Bindable
import org.mpdx.android.base.lifecycle.RealmModelViewModel
import org.mpdx.android.core.model.AccountList

class AccountListViewModel : RealmModelViewModel<AccountList>() {
    // region Model Properties
    val name get() = model?.name
    val balance get() = model?.balance
    val currency get() = model?.currency
    @get:Bindable
    val committed get() = model?.committed
    @get:Bindable
    val received get() = model?.received
    @get:Bindable
    val monthlyGoal get() = model?.monthlyGoal
    // endregion Model Properties

    // region Generated Properties
    @get:Bindable("committed", "monthlyGoal")
    val committedPercent get(): String {
        val percent = monthlyGoal?.takeIf { it > 0 }
            ?.let { goal -> ((committed ?: 0.0) * 100) / goal } ?: 0.0
        return "${percent.toInt()}%"
    }

    @get:Bindable("received", "monthlyGoal")
    val receivedPercent get(): String {
        val percent = monthlyGoal?.takeIf { it > 0 }
            ?.let { goal -> ((received ?: 0.0) * 100) / goal } ?: 0.0
        return "${percent.toInt()}%"
    }
    // endregion Generated Properties
}
