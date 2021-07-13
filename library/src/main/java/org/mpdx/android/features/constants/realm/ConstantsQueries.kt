package org.mpdx.android.features.constants.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import java.util.Locale
import org.ccci.gto.android.common.compat.util.LocaleCompat
import org.mpdx.android.features.constants.model.ConstantList
import org.mpdx.android.features.constants.model.ConstantListFields

fun Realm.getConstants(locale: Locale = Locale.getDefault()): RealmQuery<ConstantList> = where<ConstantList>()
    .equalTo(ConstantListFields.ID, LocaleCompat.toLanguageTag(locale))
