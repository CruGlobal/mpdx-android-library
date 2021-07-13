package org.mpdx.android.features.contacts.model

import com.google.gson.annotations.SerializedName
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

private const val JSON_API_TYPE_ADDRESS = "addresses"

private const val JSON_CITY = "city"
private const val JSON_COUNTRY = "country"
private const val JSON_LOCATION = "location"
private const val JSON_POSTAL_CODE = "postal_code"
private const val JSON_PRIMARY = "primary_mailing_address"
private const val JSON_STATE = "state"
private const val JSON_STREET = "street"

@JsonApiType(JSON_API_TYPE_ADDRESS)
open class Address : RealmObject(), UniqueItem, JsonApiModel, ChangeAwareItem, LocalAttributes {
    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel

    override val jsonApiType get() = JSON_API_TYPE_ADDRESS

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false

    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute(JSON_CITY)
    @SerializedName(JSON_CITY)
    var city: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_CITY)
            field = value
        }

    @JsonApiAttribute(JSON_COUNTRY)
    @SerializedName(JSON_COUNTRY)
    var country: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_COUNTRY)
            field = value
        }

    @JsonApiAttribute("end_date")
    @SerializedName("end_date")
    var endDate: String? = null

    @JsonApiAttribute(JSON_LOCATION)
    @SerializedName(JSON_LOCATION)
    var location: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_LOCATION)
            field = value
        }

    @JsonApiAttribute(JSON_POSTAL_CODE)
    @SerializedName(JSON_POSTAL_CODE)
    var postalCode: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_POSTAL_CODE)
            field = value
        }

    @JsonApiAttribute(JSON_PRIMARY)
    var isPrimary: Boolean? = false
        get() = field ?: false
        set(value) {
            if (field != value) markChanged(JSON_PRIMARY)
            field = value ?: false
        }

    @JsonApiAttribute("start_date")
    @SerializedName("start_date")
    var startDate: String? = null

    @JsonApiAttribute(JSON_STATE)
    @SerializedName(JSON_STATE)
    var state: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_STATE)
            field = value
        }

    @JsonApiAttribute(JSON_STREET)
    @SerializedName(JSON_STREET)
    var street: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_STREET)
            field = value
        }

    @JsonApiAttribute("historic")
    var isHistoric: Boolean = false

    private var source: String? = null
    // endregion Attributes

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
        if (source is Address) {
            when (field) {
                // attributes
                JSON_CITY -> city = source.city
                JSON_COUNTRY -> country = source.country
                JSON_LOCATION -> location = source.location
                JSON_POSTAL_CODE -> postalCode = source.postalCode
                JSON_PRIMARY -> isPrimary = source.isPrimary
                JSON_STATE -> state = source.state
                JSON_STREET -> street = source.street
            }
        }
    }

    override fun doesFieldMatch(original: ChangeAwareItem, field: String): Boolean {
        if (original !is Address) return false
        return when (field) {
            JSON_COUNTRY -> country == original.country
            else -> false
        }
    }

    // endregion ChangeAwareItem

    // region DBItem

    @JsonApiAttribute(JSON_ATTR_CREATED_AT)
    internal var createdAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_AT)
    private var updatedAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDatabaseAt: Date? = null

    // endregion DBItem

    // region Local Attributes

    @JsonApiIgnore
    var contact: Contact? = null

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is Address) {
            contact = contact ?: existing.contact
        }
    }

    // endregion Local Attributes

    // region Generated Attributes
    val isEditable get() = source.let { it == null || it == SOURCE_MPDX || it == SOURCE_TNT }

    val line1 get() = street
    val line2
        get() = listOfNotNull(
            city + if (city != null && state != null) "," else "",
            state,
            postalCode
        ).joinToString(" ")
    val line3 get() = country
    // endregion Generated Attributes
}
