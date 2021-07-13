package org.mpdx.android.features.appeals.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.features.contacts.model.Contact

const val JSON_API_TYPE_EXCLUDED_APPEAL_CONTACT = "excluded_appeal_contacts"

@JsonApiType(JSON_API_TYPE_EXCLUDED_APPEAL_CONTACT)
open class ExcludedAppealContact : RealmObject(), UniqueItem {
    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region Relationships
    @JsonApiAttribute("appeal")
    var appeal: Appeal? = null

    @JsonApiAttribute("contact")
    var contact: Contact? = null
    // endregion Relationships
}
