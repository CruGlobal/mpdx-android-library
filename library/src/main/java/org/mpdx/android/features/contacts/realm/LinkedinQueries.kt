package org.mpdx.android.features.contacts.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import java.util.UUID
import org.mpdx.android.features.contacts.model.LinkedInAccount
import org.mpdx.android.features.contacts.model.Person

fun Realm.getLinkedInAccounts(): RealmQuery<LinkedInAccount> = where()

fun createLinkedinAccount(person: Person? = null) = LinkedInAccount().apply {
    id = UUID.randomUUID().toString()
    this.person = person
}
