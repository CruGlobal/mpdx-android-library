package org.mpdx.android.features.appeals.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import java.util.UUID
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.features.appeals.model.Appeal
import org.mpdx.android.features.appeals.model.AskedContact
import org.mpdx.android.features.appeals.model.AskedContactFields
import org.mpdx.android.features.contacts.model.Contact

fun Realm.getAskedContacts(includeDeleted: Boolean = false) = where<AskedContact>().includeDeleted(includeDeleted)
fun Realm.getDirtyAskedContacts() = where<AskedContact>().isDirty()

fun RealmQuery<AskedContact>.forAppeal(appealId: String?): RealmQuery<AskedContact> =
    equalTo(AskedContactFields.APPEAL.ID, appealId)

fun RealmQuery<AskedContact>.hasName(): RealmQuery<AskedContact> =
    isNotNull(AskedContactFields.CONTACT.NAME).and().isNotEmpty(AskedContactFields.CONTACT.NAME)

fun RealmQuery<AskedContact>.sortByName(): RealmQuery<AskedContact> = sort(AskedContactFields.CONTACT.NAME)

fun createAskedContact(appeal: Appeal? = null, contact: Contact? = null) = AskedContact().apply {
    id = UUID.randomUUID().toString()
    isNew = true
    this.appeal = appeal
    this.contact = contact
}
