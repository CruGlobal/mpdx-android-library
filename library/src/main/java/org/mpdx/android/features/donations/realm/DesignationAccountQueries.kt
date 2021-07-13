package org.mpdx.android.features.donations.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort
import io.realm.kotlin.where
import org.mpdx.android.features.donations.model.DesignationAccount
import org.mpdx.android.features.donations.model.DesignationAccountFields

fun Realm.getDesignationAccounts(accountListId: String?): RealmQuery<DesignationAccount> = where<DesignationAccount>()
    .equalTo(DesignationAccountFields.ACCOUNT_LIST.ID, accountListId)

fun Realm.getDesignationAccount(id: String?): RealmQuery<DesignationAccount> = where<DesignationAccount>()
    .equalTo(DesignationAccountFields.ID, id)

fun RealmQuery<DesignationAccount>.withName(): RealmQuery<DesignationAccount> =
    isNotNull(DesignationAccountFields.DISPLAY_NAME).isNotEmpty(DesignationAccountFields.DISPLAY_NAME)

fun RealmQuery<DesignationAccount>.sortByName(): RealmQuery<DesignationAccount> =
    sort(DesignationAccountFields.DISPLAY_NAME, Sort.ASCENDING)
