package org.mpdx.android.features.coaching.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.core.model.AccountListFields
import org.mpdx.android.features.coaching.model.CoachingAnalytics
import org.mpdx.android.features.coaching.model.CoachingAnalyticsFields
import org.mpdx.android.features.coaching.model.CoachingAppointmentResults
import org.mpdx.android.features.coaching.model.CoachingAppointmentResultsFields
import org.threeten.bp.LocalDate

fun Realm.getCoachingAccountLists(): RealmQuery<AccountList> = where<AccountList>()
    .equalTo(AccountListFields.IS_COACHING_ACCOUNT, true)

fun Realm.getCoachingAnalytics(accountListId: String?, range: ClosedRange<LocalDate>?): RealmQuery<CoachingAnalytics> =
    where<CoachingAnalytics>()
        .equalTo(CoachingAnalyticsFields.ACCOUNT_LIST_ID, accountListId)
        .equalTo(CoachingAnalyticsFields._START_DATE, range?.start?.toString())
        .equalTo(CoachingAnalyticsFields._END_DATE, range?.endInclusive?.toString())

fun Realm.getCoachingAppointmentResults(
    accountListId: String?,
    range: ClosedRange<LocalDate>?
): RealmQuery<CoachingAppointmentResults> =
    where<CoachingAppointmentResults>()
        .equalTo(CoachingAppointmentResultsFields.ACCOUNT_LIST_ID, accountListId)
        .equalTo(CoachingAppointmentResultsFields.START_DATE, range?.start?.toEpochDay())
        .equalTo(CoachingAppointmentResultsFields.END_DATE, range?.endInclusive?.toEpochDay())
