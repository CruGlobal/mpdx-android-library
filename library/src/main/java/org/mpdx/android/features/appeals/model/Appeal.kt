package org.mpdx.android.features.appeals.model

import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.annotations.Ignore
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import java.util.Date
import kotlin.math.roundToInt
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPlaceholder
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_CREATED_AT
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.core.model.AccountList

private const val JSON_API_TYPE_APPEALS = "appeals"

private const val JSON_ACCOUNT_LIST = "account_list"
private const val JSON_CURRENCY = "total_currency"
private const val JSON_PLEDGES_NOT_RECEIVED = "pledges_amount_not_received_not_processed"
private const val JSON_PLEDGES_RECEIVED_NOT_PROCESSED = "pledges_amount_received_not_processed"
private const val JSON_PLEDGES_PROCESSED = "pledges_amount_processed"
private const val JSON_PLEDGES_TOTAL = "pledges_amount_total"

@JsonApiType(JSON_API_TYPE_APPEALS)
open class Appeal : RealmObject(), UniqueItem, JsonApiModel {
    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType: String get() = JSON_API_TYPE_APPEALS

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false
    // endregion JsonApiModel

    // region Attributes
    var name: String? = null

    private var amount: Double? = null

    @JsonApiAttribute(JSON_PLEDGES_NOT_RECEIVED)
    var pledgesNotReceived: Double? = null

    @JsonApiAttribute(JSON_PLEDGES_RECEIVED_NOT_PROCESSED)
    var pledgesReceivedNotProcessed: Double? = null

    @JsonApiAttribute(JSON_PLEDGES_PROCESSED)
    var pledgesProcessed: Double? = null

    @JsonApiAttribute(JSON_PLEDGES_TOTAL)
    private var pledgesTotal: Double? = null

    @JsonApiAttribute(JSON_CURRENCY)
    var currency: String? = null

    @JsonApiAttribute(JSON_ATTR_CREATED_AT)
    private var createdAt: Date? = null
    // endregion Attributes

    // region Relationships
    @JsonApiAttribute(JSON_ACCOUNT_LIST)
    var accountList: AccountList? = null
        private set
    // endregion Relationships

    // region Local Attributes
    @JsonApiIgnore
    @LinkingObjects("appeal")
    private val askedContacts: RealmResults<AskedContact>? = null

    fun getAskedContacts(includeDeleted: Boolean = false): RealmQuery<AskedContact>? = askedContacts?.where()
        ?.includeDeleted(includeDeleted)

    @JsonApiIgnore
    @LinkingObjects("appeal")
    val excludedContacts: RealmResults<ExcludedAppealContact>? = null
    // endregion Local Attributes

    // region Generated Attributes
    val amountInt get() = amount?.roundToInt() ?: 0
    val committedInt get() = pledgesNotReceived?.roundToInt() ?: 0
    val receivedInt get() = pledgesReceivedNotProcessed?.roundToInt() ?: 0
    val givenInt get() = pledgesProcessed?.roundToInt() ?: 0
    // endregion Generated Attributes
}
