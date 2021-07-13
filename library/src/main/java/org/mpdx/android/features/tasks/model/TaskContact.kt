package org.mpdx.android.features.tasks.model

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
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.features.contacts.model.Contact

private const val JSON_API_TYPE_TASK_CONTACT = "activity_contacts"

private const val JSON_TASK = "activity"

@JsonApiType(JSON_API_TYPE_TASK_CONTACT)
open class TaskContact : RealmObject(), UniqueItem, JsonApiModel, ChangeAwareItem {
    companion object {
        const val JSON_CONTACT = "contact"
    }

    @PrimaryKey
    @JsonApiId
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType get() = JSON_API_TYPE_TASK_CONTACT

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false
    // endregion JsonApiModel

    // region Relationships
    @JsonApiAttribute(JSON_CONTACT)
    var contact: Contact? = null
    @JsonApiAttribute(JSON_TASK, serialize = false)
    var task: Task? = null
    // endregion Relationships

    // region ChangeAwareItem
    @JsonApiIgnore
    override var isNew = false
    @JsonApiAttribute("_destroy", deserialize = false)
    override var isDeleted = false
        set(value) {
            field = value
            // HACK: clear the contact to prevent this relationship from affecting Realm Queries
            if (value) contact = null
        }
    @Ignore
    @JsonApiIgnore
    override var trackingChanges = false
    @JsonApiIgnore
    override var changedFieldsStr: String = ""
    // endregion ChangeAwareItem
}
