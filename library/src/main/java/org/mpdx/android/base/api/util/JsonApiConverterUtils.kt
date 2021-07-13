package org.mpdx.android.base.api.util

import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.json.JSONException

inline fun <reified T> JsonApiConverter.fromJsonOrNull(json: String): JsonApiObject<T>? = try {
    fromJson(json, T::class.java)
} catch (e: JSONException) {
    null
}
