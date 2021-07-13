package org.mpdx.android.core.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.model.LocalAttributes
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.base.model.addUnique

const val JSON_API_TYPE_TAG = "tags"

@JsonApiType(JSON_API_TYPE_TAG)
open class Tag : RealmObject(), UniqueItem, LocalAttributes {
    @JsonApiId
    @PrimaryKey
    override var id: String? = null
    var name: String? = null

    // region Local Attributes

    @JsonApiIgnore
    var contactTagFor = RealmList<AccountList>()

    @JsonApiIgnore
    var taskTagFor = RealmList<AccountList>()

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is Tag) {
            existing.contactTagFor.forEach { contactTagFor.addUnique(it) }
            existing.taskTagFor.forEach { taskTagFor.addUnique(it) }
        }
    }

    // endregion Local Attributes

    class Selection(val name: String, var isChecked: Boolean)
}
