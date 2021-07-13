package org.mpdx.android.core.model

import java.util.Locale
import org.ccci.gto.android.common.compat.util.LocaleCompat
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType

private const val JSON_API_TYPE_USER_DEVICES = "user_devices"

@JsonApiType(JSON_API_TYPE_USER_DEVICES)
class UserDevice(private val token: String, private val version: String?) {
    private val locale = LocaleCompat.toLanguageTag(Locale.getDefault())
    private val platform = "GCM"
}
