package org.mpdx.android.features.appeals.model

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.Date
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_IN_DB_AT
import org.mpdx.android.base.model.ChangeAwareItem
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.features.contacts.model.Contact

private const val JSON_API_TYPE_ASKED_CONTACT = "appeal_contacts"

private const val JSON_APPEAL = "appeal"

@JsonApiType(JSON_API_TYPE_ASKED_CONTACT)
open class AskedContact : RealmObject(), UniqueItem, ChangeAwareItem {
    companion object {
        const val JSON_CONTACT = "contact"
    }

    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region Attributes
    @JsonApiAttribute(name = "created_at")
    private var createdAt: Date? = null
    @JsonApiAttribute(name = "updated_at")
    private var updatedAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDatabaseAt: Date? = null

    @JsonApiAttribute("force_list_deletion", deserialize = false)
    var forceListDeletion = false
    // endregion Attributes

    // region Relationships
    @JsonApiAttribute(JSON_APPEAL)
    var appeal: Appeal? = null
    @JsonApiAttribute(JSON_CONTACT)
    var contact: Contact? = null
    // endregion Relationships

    // region Generated Attributes
    val appealId get() = appeal?.id
    val contactId get() = contact?.id
    val name get() = contact?.name
    // endregion Generated Attributes

    // region ChangeAwareItem
    @JsonApiIgnore
    override var isNew = false
    @JsonApiIgnore
    override var isDeleted = false
    @Ignore
    @JsonApiIgnore
    override var trackingChanges = false
    @JsonApiIgnore
    override var changedFieldsStr = ""
    // endregion ChangeAwareItem
}
