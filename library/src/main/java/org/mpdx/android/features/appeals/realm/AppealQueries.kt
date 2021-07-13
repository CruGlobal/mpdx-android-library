package org.mpdx.android.features.appeals.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.mpdx.android.features.appeals.model.Appeal
import org.mpdx.android.features.appeals.model.AppealFields

fun Realm.getAppeals(accountListId: String?): RealmQuery<Appeal> = where<Appeal>()
    .equalTo(AppealFields.ACCOUNT_LIST.ID, accountListId)

fun Realm.getAppeal(id: String?): RealmQuery<Appeal> = where<Appeal>()
    .equalTo(AppealFields.ID, id)

fun RealmQuery<Appeal>.hasName(): RealmQuery<Appeal> = isNotNull(AppealFields.NAME).isNotEmpty(AppealFields.NAME)
