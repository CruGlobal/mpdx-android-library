package org.mpdx.android.core.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.mpdx.android.base.model.UniqueItem

open class DbUser @JvmOverloads constructor(@PrimaryKey override var id: String? = null) : RealmObject(), UniqueItem

internal fun User.asDbUser() = DbUser(id)
