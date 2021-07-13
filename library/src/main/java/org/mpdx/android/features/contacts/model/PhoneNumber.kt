package org.mpdx.android.features.contacts.model

import android.os.Parcel
import android.os.Parcelable
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

private const val JSON_API_TYPE_PHONE_NUMBER = "phone_numbers"

private const val JSON_LOCATION = "location"
private const val JSON_NUMBER = "number"
private const val JSON_PRIMARY = "primary"
private const val JSON_HISTORIC = "historic"

@JsonApiType(JSON_API_TYPE_PHONE_NUMBER)
open class PhoneNumber() : RealmObject(), UniqueItem, JsonApiModel, ChangeAwareItem, Parcelable, LocalAttributes {
    @PrimaryKey
    @JsonApiId
    override var id: String? = null

    // region JsonApiModel

    override val jsonApiType get() = JSON_API_TYPE_PHONE_NUMBER

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false

    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute("country_code")
    private var countryCode: String? = null
    @JsonApiAttribute(JSON_LOCATION)
    var location: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_LOCATION)
            field = value
        }
    @JsonApiAttribute(JSON_NUMBER)
    var number: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_NUMBER)
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
        if (source is PhoneNumber) {
            when (field) {
                // attributes
                JSON_LOCATION -> location = source.location
                JSON_NUMBER -> number = source.number
                JSON_PRIMARY -> isPrimary = source.isPrimary
            }
        }
    }

    override fun doesFieldMatch(original: ChangeAwareItem, field: String): Boolean {
        if (original !is PhoneNumber) return false
        return when (field) {
            JSON_NUMBER -> number == original.number
            else -> false
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

    // region Local Attributes

    @JsonApiIgnore
    var person: Person? = null

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is PhoneNumber) {
            person = person ?: existing.person
        }
    }

    // endregion Local Attributes

    // region Parcelable

    protected constructor(source: Parcel) : this() {
        id = source.readString()
        countryCode = source.readString()
        location = source.readString()
        number = source.readString()
        this.source = source.readString()
        isPrimary = source.readByte().toInt() != 0
    }

    override fun describeContents() = 0
    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(id)
        parcel.writeString(countryCode)
        parcel.writeString(location)
        parcel.writeString(number)
        parcel.writeString(source)
        parcel.writeByte(if (isPrimary == true) 1 else 0)
    }

    // endregion Parcelable

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PhoneNumber> = object : Parcelable.Creator<PhoneNumber> {
            override fun createFromParcel(source: Parcel) = PhoneNumber(source)
            override fun newArray(size: Int) = arrayOfNulls<PhoneNumber>(size)
        }
    }

    // region Generated Attributes
    val isEditable get() = source.let { it == null || it == SOURCE_MPDX || it == SOURCE_TNT }
    // endregion Generated Attributes
}
