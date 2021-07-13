package org.mpdx.android.features.contacts.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPlaceholder
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.LocalAttributes
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.core.model.AccountList

private const val JSON_API_TYPE_DONOR_ACCOUNT = "donor_accounts"

@JsonApiType(JSON_API_TYPE_DONOR_ACCOUNT)
open class DonorAccount : RealmObject(), UniqueItem, JsonApiModel, LocalAttributes {
    companion object {
        const val JSON_CONTACTS = "contacts"
    }

    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType: String get() = JSON_API_TYPE_DONOR_ACCOUNT

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder: Boolean = false
    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute("account_number")
    var accountNumber: String? = null

    @JsonApiAttribute("donor_type")
    private var donorType: String? = null

    @JsonApiAttribute("first_donation_date")
    private var firstDonationDate: String? = null

    @JsonApiAttribute("last_donation_date")
    private var lastDonationDate: String? = null

    @JsonApiAttribute("total_donations")
    private var totalDonations: String? = null

    @JsonApiAttribute("display_name")
    var displayName: String? = null

    @JsonApiAttribute("created_at")
    private var createdAt: String? = null

    @JsonApiAttribute("updated_at")
    private var updatedAt: String? = null

    @JsonApiAttribute("updated_in_db_at")
    private var updatedInDatabaseAt: String? = null
    // endregion Attributes

    // region Relationships
    @JsonApiAttribute(JSON_CONTACTS, serialize = false)
    var contacts: RealmList<Contact>? = null
    val firstContact get() = contacts?.first(null)
    // endregion Relationships

    // region Local Attributes
    @JsonApiIgnore
    var accountList: AccountList? = null

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is DonorAccount) {
            accountList = accountList ?: existing.accountList
        }
    }
    // endregion Local Attributes
}
