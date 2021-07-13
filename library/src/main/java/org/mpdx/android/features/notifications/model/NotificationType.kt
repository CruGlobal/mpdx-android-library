package org.mpdx.android.features.notifications.model

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.Date
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPlaceholder
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_CREATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_IN_DB_AT
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.UniqueItem

const val JSON_API_TYPE_NOTIFICATION_TYPE = "notification_type"

@JsonApiType(JSON_API_TYPE_NOTIFICATION_TYPE)
open class NotificationType : RealmObject(), UniqueItem, JsonApiModel {
    @PrimaryKey
    @JsonApiId
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType get() = JSON_API_TYPE_NOTIFICATION_TYPE

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false
    // endregion JsonApiModel

    var description: String? = null

    @JsonApiAttribute(name = JSON_ATTR_CREATED_AT)
    private var createdAt: Date? = null
    @JsonApiAttribute(name = JSON_ATTR_UPDATED_AT)
    private var updatedAt: Date? = null
    @JsonApiAttribute(name = JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDatabaseAt: Date? = null
}
