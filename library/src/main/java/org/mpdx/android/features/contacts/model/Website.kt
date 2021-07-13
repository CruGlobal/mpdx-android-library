package org.mpdx.android.features.contacts.model

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPlaceholder
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.model.ChangeAwareItem
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.LocalAttributes
import org.mpdx.android.base.model.UniqueItem

private const val JSON_API_TYPE_WEBSITE = "websites"

private const val JSON_URL = "url"

@JsonApiType(JSON_API_TYPE_WEBSITE)
open class Website : RealmObject(), UniqueItem, JsonApiModel, ChangeAwareItem, LocalAttributes {
    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType get() = JSON_API_TYPE_WEBSITE

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false
    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute(JSON_URL)
    var url: String? = null
        set(value) {
            if (field.orEmpty() != value.orEmpty()) markChanged(JSON_URL)
            field = value
        }
    // endregion Attributes

    // region Local Attributes
    @JsonApiIgnore
    var person: Person? = null

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is Website) {
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
        if (source is Website) {
            when (field) {
                // attributes
                JSON_URL -> url = source.url
            }
        }
    }
    // endregion ChangeAwareItem
}
