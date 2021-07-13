package org.mpdx.android.features.contacts.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import java.util.UUID
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.model.TwitterAccount

fun Realm.getTwitterAccounts(): RealmQuery<TwitterAccount> = where()

fun createTwitterAccount(person: Person? = null) = TwitterAccount().apply {
    id = UUID.randomUUID().toString()
    this.person = person
}
