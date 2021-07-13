package org.mpdx.android.features.dashboard.model

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPostCreate
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.core.model.AccountList

private const val JSON_API_TYPE_GOAL_PROGRESS = "reports_goal_progresses"

private const val JSON_ACCOUNT_LIST = "account_list"

@JsonApiType(JSON_API_TYPE_GOAL_PROGRESS)
open class GoalProgress : RealmObject(), UniqueItem {
    @PrimaryKey
    @JsonApiIgnore
    override var id: String? = null
        get() = field ?: accountList?.id ?: ""

    // region Attributes
    @JsonApiAttribute("in_hand_percent")
    var inHandPercent: Double? = null
        get() = field ?: 0.0

    @JsonApiAttribute("monthly_goal")
    var monthlyGoal: Double = 0.0

    @JsonApiAttribute("pledged_percent")
    var pledgedPercent: Double? = null
        get() = field ?: 0.0

    @JsonApiAttribute("received_pledges")
    var receivedPledges: Double? = null
        get() = field ?: 0.0

    @JsonApiAttribute("balance")
    var salaryBalance: Double = 0.0

    @JsonApiAttribute("default_currency")
    var salaryCurrencyOrDefault: String? = null

    @JsonApiAttribute("total_pledges")
    var totalPledges: Double? = null
        get() = field ?: 0.0
    // endregion Attributes

    // region Relationships
    @Ignore
    @JsonApiAttribute(JSON_ACCOUNT_LIST)
    private var accountList: AccountList? = null
    // endregion Relationships

    // region API logic
    @JsonApiPostCreate
    private fun populateId() {
        id = accountList?.id ?: id
    }
    // endregion API logic
}
