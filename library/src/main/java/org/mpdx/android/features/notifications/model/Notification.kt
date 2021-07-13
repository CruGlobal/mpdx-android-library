package org.mpdx.android.features.notifications.model

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.Date
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPlaceholder
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPostCreate
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_CREATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_AT
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_IN_DB_AT
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.donations.model.Donation

private const val JSON_API_TYPE_NOTIFICATION = "notifications"

private const val JSON_CLEARED = "cleared"
private const val JSON_DONATION = "donation"
private const val JSON_EVENT_DATE = "event_date"

@JsonApiType(JSON_API_TYPE_NOTIFICATION)
open class Notification : RealmObject(), UniqueItem, JsonApiModel {
    companion object {
        const val JSON_CONTACT = "contact"
        const val JSON_NOTIFICATION_TYPE = "notification_type"
    }

    @PrimaryKey
    @JsonApiId
    override var id: String? = null

    // region JsonApiModel
    override val jsonApiType get() = JSON_API_TYPE_NOTIFICATION

    @Ignore
    @JsonApiPlaceholder
    override var isPlaceholder = false
    // endregion JsonApiModel

    @JsonApiAttribute(JSON_CLEARED)
    var isCleared: Boolean = false

    @JsonApiAttribute(JSON_EVENT_DATE)
    var eventDate: Date? = null

    // region Relationships
    @JsonApiAttribute(JSON_CONTACT)
    var contact: Contact? = null
        private set

    @JsonApiAttribute(JSON_NOTIFICATION_TYPE)
    var notificationType: NotificationType? = null

    @Ignore
    @JsonApiAttribute(JSON_DONATION)
    private var donation: Donation? = null
    @JsonApiIgnore
    private var donationId: String? = null
        get() = field ?: donation?.id
        set(value) {
            donation = null
            field = value
        }
    // endregion Relationships

    @JsonApiAttribute(JSON_ATTR_CREATED_AT)
    private var createdAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_AT)
    private var updatedAt: Date? = null
    @JsonApiAttribute(JSON_ATTR_UPDATED_IN_DB_AT)
    private var updatedInDatabaseAt: Date? = null

    @JsonApiPostCreate
    private fun flattenDonation() {
        donation?.let {
            donationId = it.id
        }
    }
}
