package org.mpdx.android.features.settings.model

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
import org.mpdx.android.features.notifications.model.NotificationType

private const val JSON_API_TYPE_NOTIFICATION_PREFERENCE = "notification_preferences"

private const val JSON_APP = "app"
private const val JSON_EMAIL = "email"
private const val JSON_TASK = "task"

@JsonApiType(JSON_API_TYPE_NOTIFICATION_PREFERENCE)
open class NotificationPreference : RealmObject(), UniqueItem, JsonApiModel, ChangeAwareItem, LocalAttributes {
    companion object {
        const val JSON_NOTIFICATION_TYPE = "notification_type"
    }

    @PrimaryKey
    @JsonApiId
    override var id: String? = null

    // region JsonApiModel

    override val jsonApiType get() = JSON_API_TYPE_NOTIFICATION_PREFERENCE

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false

    // endregion JsonApiModel

    // region Attributes

    @JsonApiAttribute(JSON_APP)
    var isEnabledForApp: Boolean = false
        set(value) {
            if (value != field) markChanged(JSON_APP)
            field = value
        }
    @JsonApiAttribute(JSON_EMAIL)
    var isEnabledForEmail: Boolean = false
    @JsonApiAttribute(JSON_TASK)
    var isEnabledForTask: Boolean = false

    // endregion Attributes

    // region Relationships

    @JsonApiAttribute(JSON_NOTIFICATION_TYPE)
    var notificationType: NotificationType? = null

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
    override var changedFieldsStr: String = ""

    override fun mergeChangedField(source: ChangeAwareItem, field: String) {
        if (source is NotificationPreference) {
            when (field) {
                JSON_APP -> isEnabledForApp = source.isEnabledForApp
            }
        }
    }

    // endregion ChangeAwareItem

    // region DBItem

    @JsonApiAttribute(JSON_ATTR_CREATED_AT)
    private var createdAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_AT)
    private var updatedAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDatabaseAt: Date? = null

    // endregion DBItem

    // region Local Attributes

    @JsonApiIgnore
    var accountListId: String? = null

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is NotificationPreference) {
            accountListId = accountListId ?: existing.accountListId
        }
    }

    // endregion Local Attributes
}
