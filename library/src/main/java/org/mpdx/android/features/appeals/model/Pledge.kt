package org.mpdx.android.features.appeals.model

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Ignore
import io.realm.annotations.LinkingObjects
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
import org.mpdx.android.base.model.ChangeAwareItem
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.donations.model.Donation

const val JSON_API_TYPE_PLEDGE = "pledges"

private const val JSON_ACCOUNT_LIST = "account_list"
private const val JSON_APPEAL = "appeal"
private const val JSON_AMOUNT = "amount"
private const val JSON_CONTACT = "contact"
private const val JSON_EXPECTED_DATE = "expected_date"
private const val JSON_STATUS = "status"

@JsonApiType(JSON_API_TYPE_PLEDGE)
open class Pledge : RealmObject(), UniqueItem, ChangeAwareItem, JsonApiModel {
    companion object {
        const val JSON_DONATIONS = "donations"

        const val JSON_FILTER_APPEAL_ID = "appeal_id"

        const val STATUS_COMMITTED = "not_received"
        const val STATUS_RECEIVED_NOT_PROCESSED = "received_not_processed"
        const val JSON_VALUE_STATUS_GIVEN = "processed"
    }

    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType: String get() = JSON_API_TYPE_PLEDGE

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false
    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute(JSON_AMOUNT)
    var amount: Double? = null
        set(value) {
            if (value != field) markChanged(JSON_AMOUNT)
            field = value
        }

    @JsonApiAttribute(JSON_STATUS)
    var status: String? = null
        set(value) {
            if (value != field) markChanged(JSON_STATUS)
            field = value
        }

    @JsonApiAttribute(JSON_EXPECTED_DATE)
    var expectedDate: Date? = null
        set(value) {
            if (value != field) markChanged(JSON_EXPECTED_DATE)
            field = value
        }
    // endregion Attributes

    var isReceived: Boolean
        get() = status == STATUS_RECEIVED_NOT_PROCESSED || status == JSON_VALUE_STATUS_GIVEN
        set(value) {
            status = if (value) STATUS_RECEIVED_NOT_PROCESSED else STATUS_COMMITTED
        }

    // region Relationships
    @JsonApiAttribute(JSON_ACCOUNT_LIST, serialize = false)
    var accountList: AccountList? = null

    @JsonApiAttribute(JSON_APPEAL)
    var appeal: Appeal? = null
        set(value) {
            if (value?.id != field?.id) markChanged(JSON_APPEAL)
            field = value
        }

    @JsonApiAttribute(JSON_CONTACT)
    var contact: Contact? = null
        set(value) {
            if (value?.id != field?.id) markChanged(JSON_CONTACT)
            field = value
        }

    val contactId: String? get() = contact?.id

    @Ignore
    @JsonApiAttribute(JSON_DONATIONS, serialize = false)
    internal var apiDonations: List<Donation>? = null
        private set
    @JsonApiIgnore
    @LinkingObjects("pledge")
    val donations: RealmResults<Donation>? = null
    // endregion Relationships

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
        if (source is Pledge) {
            when (field) {
                JSON_AMOUNT -> amount = source.amount
                JSON_APPEAL -> appeal = source.appeal
                JSON_CONTACT -> contact = source.contact
                JSON_EXPECTED_DATE -> expectedDate = source.expectedDate
                JSON_STATUS -> status = source.status
            }
        }
    }
    // endregion ChangeAwareItem

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
    private fun populateDonationPledgeReferenceIfNecessary() = apiDonations?.forEach { it.pledge = it.pledge ?: this }
    // endregion API logic
}
