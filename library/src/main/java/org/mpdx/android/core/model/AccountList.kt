package org.mpdx.android.core.model

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.Date
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPlaceholder
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPostCreate
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_CREATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_IN_DB_AT
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.LocalAttributes
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.features.appeals.model.Appeal

private const val JSON_API_TYPE_ACCOUNT_LIST = "account_lists"

@JvmField
val COMPARATOR_ACCOUNT_LIST_NAME = compareBy<AccountList?> { it?.name }

@JsonApiType(JSON_API_TYPE_ACCOUNT_LIST)
open class AccountList : RealmObject(), UniqueItem, JsonApiModel, LocalAttributes {
    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel

    override val jsonApiType get() = JSON_API_TYPE_ACCOUNT_LIST

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false

    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute("currency")
    var currency: String? = null

    @JsonApiAttribute("monthly_goal")
    var monthlyGoal: Double? = null

    @JsonApiAttribute("name")
    var name: String? = null

    @JsonApiAttribute("active_mpd_finish_at")
    var activeFinishAt: Date? = null

    @JsonApiAttribute("active_mpd_monthly_goal")
    var activeMonthlyGoal: String? = null

    @JsonApiAttribute("active_mpd_start_at")
    var activeStartAt: Date? = null

    @JsonApiAttribute("last_prayer_letter_at")
    var lastPrayerLetterAt: String? = null

    @JsonApiAttribute("balance")
    var balance: Double? = null

    @JsonApiAttribute("committed")
    var committed: Double? = null

    @JsonApiAttribute("received")
    var received: Double? = null
    // endregion Attributes

    // region Relationships
    @Ignore
    @JsonApiAttribute("primary_appeal")
    private var primaryAppeal: Appeal? = null
    var primaryAppealId: String? = null
        private set
    // endregion Relationships

    // region DBItem

    @JsonApiAttribute(JSON_ATTR_CREATED_AT)
    private var createdAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_AT)
    private var updatedAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDatabaseAt: Date? = null

    // endregion DBItem

    // region API logic

    @JsonApiPostCreate
    private fun flattenPrimaryAppeal() {
        primaryAppeal?.let {
            primaryAppealId = it.id
        }
    }

    // endregion API logic

    // region Local Attributes

    @JsonApiIgnore
    var isUserAccount = false

    @JsonApiIgnore
    var isCoachingAccount = false

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is AccountList) {
            isUserAccount = isUserAccount || existing.isUserAccount
            isCoachingAccount = isCoachingAccount || existing.isCoachingAccount
        }
    }

    // endregion Local Attributes
}

fun AccountList?.orPlaceholder(accountListId: String) = this ?: AccountList().apply {
    id = accountListId
    isPlaceholder = true
}
