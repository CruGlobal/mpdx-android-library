package org.mpdx.android.core.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.core.model.AccountListFields
import org.mpdx.android.features.contacts.model.DonorAccount
import org.mpdx.android.features.contacts.model.DonorAccountFields

// region AccountList

fun Realm.getAccountLists(): RealmQuery<AccountList> = where<AccountList>()
    .equalTo(AccountListFields.IS_USER_ACCOUNT, true)

fun Realm.getAccountList(id: String?): RealmQuery<AccountList> = where<AccountList>()
    .equalTo(AccountListFields.ID, id)

// endregion AccountList

// region DonorAccount (PartnerAccount)

fun Realm.getDonorAccounts(accountListId: String?): RealmQuery<DonorAccount> = where<DonorAccount>()
    .equalTo(DonorAccountFields.ACCOUNT_LIST.ID, accountListId)

fun Realm.getDonorAccount(id: String?): RealmQuery<DonorAccount> = where<DonorAccount>()
    .equalTo(DonorAccountFields.ID, id)

// endregion DonorAccount
