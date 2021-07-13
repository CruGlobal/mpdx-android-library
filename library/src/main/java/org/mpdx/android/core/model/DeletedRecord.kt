package org.mpdx.android.core.model

import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType

private const val JSON_API_TYPE_DELETED_RECORDS = "deleted_records"

@JsonApiType(JSON_API_TYPE_DELETED_RECORDS)
class DeletedRecord {
    companion object {
        const val TYPE_CONTACT = "Contact"
        const val TYPE_TASK = "Task"
    }

    @JsonApiAttribute("deletable_id")
    var deletableId: String? = null
        private set
}
