package org.mpdx.android.features.dashboard.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.mpdx.android.features.dashboard.model.GoalProgress
import org.mpdx.android.features.dashboard.model.GoalProgressFields

fun Realm.getGoalProgress() = where<GoalProgress>()

fun RealmQuery<GoalProgress>.forAccountList(accountListId: String?): RealmQuery<GoalProgress> =
    equalTo(GoalProgressFields.ID, accountListId)
