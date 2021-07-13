package org.mpdx.android.features.contacts.api.converter

import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton
import org.ccci.gto.android.common.jsonapi.converter.TypeConverter
import org.mpdx.android.core.model.UserPreferences

@Singleton
internal class UserPreferencesConverter @Inject constructor(moshi: Moshi) : TypeConverter<UserPreferences> {
    private val adapter = moshi.adapter(UserPreferences::class.java)

    override fun supports(clazz: Class<*>) = clazz == UserPreferences::class.java
    override fun fromString(value: String?) = value?.let { adapter.fromJson(value) }
    override fun toString(value: UserPreferences?): String? = adapter.toJson(value)
}
