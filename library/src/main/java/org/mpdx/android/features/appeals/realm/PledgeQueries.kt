package org.mpdx.android.features.appeals.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.features.appeals.model.Pledge
import org.mpdx.android.features.appeals.model.PledgeFields

fun Realm.getPledges(includeDeleted: Boolean = false): RealmQuery<Pledge> = where<Pledge>()
    .includeDeleted(includeDeleted)

fun RealmQuery<Pledge>.forAppeal(appealId: String?): RealmQuery<Pledge> = equalTo(PledgeFields.APPEAL.ID, appealId)
fun RealmQuery<Pledge>.status(status: String?): RealmQuery<Pledge> = equalTo(PledgeFields.STATUS, status)

fun RealmQuery<Pledge>.sortByName(): RealmQuery<Pledge> = sort(PledgeFields.CONTACT.NAME)
