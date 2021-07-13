package org.mpdx.android.features.contacts.model

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.Date
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPlaceholder
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_CREATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_IN_DB_AT
import org.mpdx.android.base.model.ChangeAwareItem
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.LocalAttributes
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.features.constants.Constants.SOURCE_MPDX
import org.mpdx.android.features.constants.Constants.SOURCE_TNT

private const val JSON_API_TYPE_EMAIL_ADDRESS = "email_addresses"
private const val JSON_HISTORIC = "historic"
private const val JSON_LOCATION = "location"
private const val JSON_PRIMARY = "primary"

@JsonApiType(JSON_API_TYPE_EMAIL_ADDRESS)
open class EmailAddress : RealmObject(), UniqueItem, JsonApiModel, ChangeAwareItem, LocalAttributes {
    companion object {
        const val JSON_EMAIL = "email"
    }
    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel

    override val jsonApiType get() = JSON_API_TYPE_EMAIL_ADDRESS

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false

    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute(JSON_EMAIL)
    var email: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_EMAIL)
            field = value
        }

    @JsonApiAttribute(JSON_LOCATION)
    var location: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_LOCATION)
            field = value
        }
    @JsonApiAttribute(JSON_PRIMARY)
    var isPrimary: Boolean? = false
        set(value) {
            if (field != value) markChanged(JSON_PRIMARY)
            field = value ?: false
        }

    private var source: String? = null
    @JsonApiAttribute(JSON_HISTORIC)
    var isHistoric = false
    // endregion Attributes

    // region Local Attributes

    @JsonApiIgnore
    var person: Person? = null

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is EmailAddress) {
            person = person ?: existing.person
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
    override var changedFieldsStr = ""

    override fun mergeChangedField(source: ChangeAwareItem, field: String) {
        if (source is EmailAddress) {
            when (field) {
                // attributes
                JSON_EMAIL -> email = source.email
                JSON_LOCATION -> location = source.location
                JSON_PRIMARY -> isPrimary = source.isPrimary
            }
        }
    }

    // endregion ChangeAwareItem

    // region DBItem

    @JsonApiAttribute(JSON_ATTR_CREATED_AT)
    var createdAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_AT)
    private var updatedAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDatabaseAt: Date? = null

    // endregion DBItem

    // region Generated Attributes
    val isEditable get() = source.let { it == null || it == SOURCE_MPDX || it == SOURCE_TNT }
    // endregion Generated Attributes
}
