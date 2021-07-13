package org.mpdx.android.features.notifications.model

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

private const val JSON_API_TYPE_USER_NOTIFICATION = "user_notifications"
private const val JSON_READ = "read"

@JsonApiType(JSON_API_TYPE_USER_NOTIFICATION)
open class UserNotification : RealmObject(), UniqueItem, ChangeAwareItem, JsonApiModel {
    companion object {
        const val JSON_NOTIFICATION = "notification"
    }

    @PrimaryKey
    @JsonApiId
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType get() = JSON_API_TYPE_USER_NOTIFICATION

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false
    // endregion JsonApiModel

    // region Attributes
    @JsonApiAttribute(JSON_READ)
    var isRead: Boolean = false
        set(value) {
            if (value != field) markChanged(JSON_READ)
            field = value
        }
    // endregion Attributes

    // region Relationships
    @JsonApiAttribute(JSON_NOTIFICATION)
    var notification: Notification? = null
    // endregion Relationships

    @JsonApiAttribute(JSON_ATTR_CREATED_AT)
    private var createdAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_AT)
    private var updatedAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDatabaseAt: Date? = null

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
        if (source is UserNotification) {
            when (field) {
                JSON_READ -> isRead = source.isRead
            }
        }
    }
    // endregion ChangeAwareItem
}
