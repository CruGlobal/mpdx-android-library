package org.mpdx.android.features.donations.model

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.Date
import java.util.Locale
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPlaceholder
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPostCreate
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_CREATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_IN_DB_AT
import org.mpdx.android.base.model.ChangeAwareItem
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.LocalAttributes
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.features.appeals.model.Appeal
import org.mpdx.android.features.appeals.model.Pledge
import org.mpdx.android.features.contacts.model.DonorAccount

const val JSON_API_TYPE_DONATION = "donations"

private const val JSON_PLEDGE = "pledge"

@JvmField
val DONATION_COMPARATOR_DATE = compareBy<Donation?> { it?.donationDate ?: Date() }

@JsonApiType(JSON_API_TYPE_DONATION)
open class Donation : RealmObject(), UniqueItem, ChangeAwareItem, JsonApiModel, LocalAttributes {
    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType: String get() = JSON_API_TYPE_DONATION

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder: Boolean = false
    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute("type")
    private var type: String? = null

    @JsonApiAttribute(JSON_AMOUNT)
    var amount: Double? = null
        set(value) {
            if (value != field) markChanged(JSON_AMOUNT)
            field = value
        }

    @JsonApiAttribute(JSON_APPEAL_AMOUNT)
    var appealAmount: String? = null
        set(value) {
            if (value != field) markChanged(JSON_APPEAL_AMOUNT)
            field = value
        }

    @JsonApiAttribute("converted_appeal_amount")
    var convertedAppealAmount: Double? = null

    @JsonApiAttribute(JSON_CONVERTED_AMOUNT)
    var convertedAmount: Double? = null

    @JsonApiAttribute(JSON_CURRENCY)
    var currency: String? = null
        set(value) {
            if (value != field) markChanged(JSON_CURRENCY)
            field = value
        }

    @JsonApiAttribute(JSON_CONVERTED_CURRENCY)
    var convertedCurrency: String? = null

    @JsonApiAttribute(JSON_DONATION_DATE)
    var donationDate: Date? = null
        set(value) {
            if (value != field) markChanged(JSON_DONATION_DATE)
            field = value
        }

    @JsonApiAttribute(JSON_MEMO)
    var memo: String? = null
        set(value) {
            if (value != field) markChanged(JSON_MEMO)
            field = value
        }

    @JsonApiAttribute("motivation")
    var motivation: String? = null

    @JsonApiAttribute("payment_method")
    private var paymentMethod: String? = null

    @JsonApiAttribute("remote_id")
    private var remoteId: String? = null

    @JsonApiAttribute("tendered_amount")
    var tenderedAmount: String? = null
        get() = String.format(Locale.getDefault(), "%.2f", field?.toDoubleOrNull() ?: 0f)

    @JsonApiAttribute("tendered_currency")
    private var tenderedCurrency: String? = null
    // endregion Attributes

    // region Relationships
    @Ignore
    @JsonApiAttribute(JSON_APPEAL)
    private var appeal: Appeal? = null
    @JsonApiIgnore
    var appealId: String? = null
        get() = field ?: appeal?.id
        set(value) {
            if (value != field) markChanged(JSON_APPEAL)
            field = value
            appeal = null
        }

    @Ignore
    @JsonApiAttribute(JSON_DESIGNATION_ACCOUNT)
    private var designationAccount: DesignationAccount? = null
    @JsonApiIgnore
    var designationId: String? = null
        get() = field ?: designationAccount?.id
        set(value) {
            if (value != field) markChanged(JSON_DESIGNATION_ACCOUNT)
            field = value
            designationAccount = null
        }

    @JsonApiAttribute(JSON_DONOR_ACCOUNT)
    var donorAccount: DonorAccount? = null
        set(value) {
            if (value?.id != field?.id) markChanged(JSON_DONOR_ACCOUNT)
            field = value
        }
    val contact get() = donorAccount?.firstContact

    @JsonApiAttribute(JSON_PLEDGE)
    var pledge: Pledge? = null
    // endregion Relationships

    // region Local Attributes
    @JsonApiIgnore
    var accountList: AccountList? = null

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is Donation) {
            accountList = accountList ?: existing.accountList
        }
    }
    // endregion Local Attributes

    // region ChangeAwareItem
    @JsonApiIgnore
    override var isNew = false
    @JsonApiIgnore
    override var isDeleted = false
    @Ignore
    @JsonApiIgnore
    override var trackingChanges = false
    @JsonApiIgnore
    override var changedFieldsStr: String = ""

    override fun mergeChangedField(source: ChangeAwareItem, field: String) {
        if (source is Donation) {
            when (field) {
                JSON_AMOUNT -> amount = source.amount
                JSON_APPEAL -> appealId = source.appealId
                JSON_APPEAL_AMOUNT -> appealAmount = source.appealAmount
                JSON_CONVERTED_AMOUNT -> convertedAmount = source.convertedAmount
                JSON_CONVERTED_CURRENCY -> convertedCurrency = source.convertedCurrency
                JSON_CURRENCY -> currency = source.currency
                JSON_DESIGNATION_ACCOUNT -> designationId = source.designationId
                JSON_DONATION_DATE -> donationDate = source.donationDate
                JSON_DONOR_ACCOUNT -> donorAccount = source.donorAccount
                JSON_MEMO -> memo = source.memo
            }
        }
    }
    // endregion ChangeAwareItem

    // region DBItem
    @JsonApiAttribute(name = JSON_ATTR_CREATED_AT)
    private var createdAt: Date? = null
    @JsonApiAttribute(name = JSON_ATTR_UPDATED_AT)
    private var updatedAt: Date? = null
    @JsonApiAttribute(name = JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDatabaseAt: Date? = null
    // endregion DBItem

    // region API logic
    @JsonApiPostCreate
    private fun flattenAppeal() {
        appeal?.let {
            appealId = it.id
            appeal = null
        }
    }

    @JsonApiPostCreate
    private fun flattenDesignationAccount() {
        designationAccount?.let {
            designationId = it.id
            designationAccount = null
        }
    }

    override fun prepareForApi() {
        inflateAppeal()
        inflateDesignationAccount()
    }

    private fun inflateAppeal() = appealId?.let { appealId ->
        appeal = Appeal().apply { id = appealId }
    }

    private fun inflateDesignationAccount() = designationId?.let { designationId ->
        designationAccount = DesignationAccount().apply { id = designationId }
    }
    // endregion API logic

    override fun equals(other: Any?): Boolean {
        return other is Donation && id == other.id
    }

    override fun hashCode(): Int {
        return id!!.hashCode()
    }

    companion object {
        const val JSON_AMOUNT = "amount"
        const val JSON_APPEAL = "appeal"
        const val JSON_APPEAL_AMOUNT = "appeal_amount"
        const val JSON_CONVERTED_AMOUNT = "converted_amount"
        const val JSON_CONVERTED_CURRENCY = "converted_currency"
        const val JSON_CURRENCY = "currency"
        const val JSON_DESIGNATION_ACCOUNT = "designation_account"
        const val JSON_DONOR_ACCOUNT = "donor_account"
        const val JSON_DONATION_DATE = "donation_date"
        const val JSON_DONOR_ACCOUNT_ID = "donor_account_id"
        const val JSON_MEMO = "memo"
    }
}
