package org.mpdx.android.features.constants.api.converter

import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton
import org.ccci.gto.android.common.jsonapi.converter.TypeConverter
import org.mpdx.android.features.constants.model.CruHashes

@Singleton
class CruHashesConverter @Inject constructor(moshi: Moshi) : TypeConverter<CruHashes> {
    private val adapter = moshi.adapter(CruHashes::class.java)

    override fun supports(clazz: Class<*>) = CruHashes::class.java == clazz
    override fun fromString(value: String?) = value?.let { adapter.fromJson(value) }
    override fun toString(value: CruHashes?): String? = adapter.toJson(value)
}
