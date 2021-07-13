package org.mpdx.android.features.settings.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.features.settings.model.NotificationPreference
import org.mpdx.android.features.settings.model.NotificationPreferenceFields

fun Realm.getNotificationPreferences(accountListId: String?): RealmQuery<NotificationPreference> =
    where<NotificationPreference>().equalTo(NotificationPreferenceFields.ACCOUNT_LIST_ID, accountListId)

fun Realm.getNotificationPreference(id: String?): RealmQuery<NotificationPreference> =
    where<NotificationPreference>().equalTo(NotificationPreferenceFields.ID, id)

fun Realm.getDirtyNotificationPreferences(): RealmQuery<NotificationPreference> =
    where<NotificationPreference>().isDirty()
