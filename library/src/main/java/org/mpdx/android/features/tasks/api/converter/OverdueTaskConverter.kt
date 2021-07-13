package org.mpdx.android.features.tasks.api.converter

import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton
import org.ccci.gto.android.common.jsonapi.converter.TypeConverter
import org.mpdx.android.features.tasks.model.OverdueTask

@Singleton
class OverdueTaskConverter @Inject constructor(moshi: Moshi) : TypeConverter<OverdueTask> {
    private val adapter = moshi.adapter(OverdueTask::class.java)

    override fun supports(clazz: Class<*>) = OverdueTask::class.java == clazz
    override fun fromString(value: String?) = value?.let { adapter.fromJson(value) }
    override fun toString(value: OverdueTask?): String? = adapter.toJson(value)
}
