package org.mpdx.android.features.donations.model

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

private const val JSON_API_TYPE_DESIGNATION_ACCOUNT = "designation_accounts"

@JsonApiType(JSON_API_TYPE_DESIGNATION_ACCOUNT)
open class DesignationAccount : RealmObject(), UniqueItem, JsonApiModel, LocalAttributes {
    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType get() = JSON_API_TYPE_DESIGNATION_ACCOUNT

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false
    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute("display_name")
    var displayName: String? = null
    // endregion Attributes

    // region Local Attributes
    @JsonApiIgnore
    var accountList: AccountList? = null

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is DesignationAccount) {
            accountList = accountList ?: existing.accountList
        }
    }
    // endregion Local Attributes
}
