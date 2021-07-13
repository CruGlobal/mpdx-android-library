package org.mpdx.android.core.api.converter

import java.util.Date
import org.ccci.gto.android.common.jsonapi.converter.TypeConverter
import org.mpdx.android.utils.toBpInstant
import org.mpdx.android.utils.toDate
import org.mpdx.android.utils.toInstanOrNull
import org.mpdx.android.utils.toLocalDateOrNull
import org.mpdx.android.utils.withoutFractionalSeconds

object DateTypeConverter : TypeConverter<Date> {
    override fun supports(clazz: Class<*>) = clazz == Date::class.java
    override fun toString(value: Date?) = value?.toBpInstant()?.withoutFractionalSeconds()?.toString()
    override fun fromString(value: String?) = value?.toInstanOrNull()?.toDate() ?: value?.toLocalDateOrNull()?.toDate()
}
