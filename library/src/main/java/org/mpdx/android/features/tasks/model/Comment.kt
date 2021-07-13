package org.mpdx.android.features.tasks.model

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
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.features.contacts.model.Person

const val JSON_API_TYPE_COMMENT = "comments"

private const val JSON_BODY = "body"

@JsonApiType(JSON_API_TYPE_COMMENT)
open class Comment : RealmObject(), UniqueItem, JsonApiModel, ChangeAwareItem {
    companion object {
        const val JSON_PERSON = "person"
    }

    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType get() = JSON_API_TYPE_COMMENT

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false
    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute(JSON_BODY)
    var body: String? = null
        set(value) {
            if (value.orEmpty() != field.orEmpty()) markChanged(JSON_BODY)
            field = value
        }
    // endregion Attributes

    // region Relationships
    @JsonApiIgnore
    var task: Task? = null

    @JsonApiAttribute(JSON_PERSON)
    var person: Person? = null
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
    override var changedFieldsStr = ""

    override fun mergeChangedField(source: ChangeAwareItem, field: String) {
        if (source is Comment) {
            when (field) {
                JSON_BODY -> body = source.body
            }
        }
    }
    // endregion ChangeAwareItem

    // region DBItem
    @JsonApiAttribute(name = JSON_ATTR_CREATED_AT)
    var createdAt: Date? = null
    @JsonApiAttribute(name = JSON_ATTR_UPDATED_AT)
    private var updatedAt: Date? = null
    @JsonApiAttribute(name = JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDbAt: Date? = null
    // endregion DBItem
}
