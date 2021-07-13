package org.mpdx.android.features.contacts.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import java.util.UUID
import org.mpdx.android.features.contacts.model.FacebookAccount
import org.mpdx.android.features.contacts.model.Person

fun Realm.getFacebookAccounts(): RealmQuery<FacebookAccount> = where()

fun createFacebookAccount(person: Person? = null) = FacebookAccount().apply {
    id = UUID.randomUUID().toString()
    this.person = person
}
