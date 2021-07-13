package org.mpdx.android.core.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiId
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.model.LocalAttributes
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.base.model.addUnique

private const val JSON_API_TYPE_USER = "users"

private const val JSON_FIRST_NAME = "first_name"
private const val JSON_LAST_NAME = "last_name"
private const val JSON_AVATAR = "avatar"

@JsonApiType(JSON_API_TYPE_USER)
open class User : RealmObject(), UniqueItem, LocalAttributes {
    @JsonApiId
    @PrimaryKey
    override var id: String? = null

    @JsonApiAttribute(JSON_FIRST_NAME)
    var firstName: String? = null
    @JsonApiAttribute(JSON_LAST_NAME)
    var lastName: String? = null

    @Ignore
    @JsonApiAttribute(JSON_AVATAR)
    var avatar: String? = null

    // region LocalAttributes
    @JsonApiIgnore
    var accountLists = RealmList<AccountList>()

    override fun mergeInLocalAttributes(existing: LocalAttributes) {
        if (existing is User) {
            existing.accountLists.forEach { accountLists.addUnique(it) }
        }
    }
    // endregion LocalAttributes

    @Ignore
    @JsonApiAttribute("preferences")
    internal var preferences: UserPreferences? = null
    val defaultAccountList get() = preferences?.defaultAccountList
    val setupStatus get() = preferences?.setupStatus
}

@JsonClass(generateAdapter = true)
internal open class UserPreferences {
    @Json(name = "default_account_list")
    var defaultAccountList: String? = null
    @Json(name = "setup")
    var setupStatus: String? = null
}
