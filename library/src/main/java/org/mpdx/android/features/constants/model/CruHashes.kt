package org.mpdx.android.features.constants.model

import com.squareup.moshi.JsonClass
import io.realm.RealmModel
import io.realm.annotations.RealmClass

@RealmClass
@JsonClass(generateAdapter = true)
open class CruHashes : RealmModel {
    var id: String? = null
    var key: String? = null
    var value: String? = null
}
