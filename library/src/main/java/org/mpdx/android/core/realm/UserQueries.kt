package org.mpdx.android.core.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.mpdx.android.core.model.User
import org.mpdx.android.core.model.UserFields

fun Realm.getUsers() = where<User>()
fun Realm.getUser(userId: String?): RealmQuery<User> = getUsers().equalTo(UserFields.ID, userId)

fun RealmQuery<User>.forAccountList(accountListId: String?): RealmQuery<User> =
    equalTo(UserFields.ACCOUNT_LISTS.ID, accountListId)
