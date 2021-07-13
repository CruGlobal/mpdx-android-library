package org.mpdx.android.features.coaching.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.base.TimeConstants.DAY_IN_MS
import org.ccci.gto.android.common.base.TimeConstants.HOUR_IN_MS
import org.ccci.gto.android.common.base.TimeConstants.WEEK_IN_MS
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.util.asApiDateRange
import org.mpdx.android.base.api.util.filter
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.base.sync.model.LastSyncTime
import org.mpdx.android.features.coaching.api.CoachingApi
import org.mpdx.android.features.coaching.api.JSON_FILTER_APPOINTMENTS_RANGE
import org.mpdx.android.features.coaching.api.JSON_FILTER_DATE_RANGE
import org.mpdx.android.features.coaching.model.CoachingAccountList
import org.mpdx.android.features.coaching.model.CoachingAppointmentResults
import org.mpdx.android.features.coaching.realm.getCoachingAccountLists
import org.mpdx.android.utils.realmTransaction
import org.mpdx.android.utils.toBpInstant
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber

private const val TAG = "CoachingSyncService"

private const val SUBTYPE_COACHING_ACCOUNT_LISTS = "coaching_account_lists"
private const val SUBTYPE_COACHING_ANALYTICS = "coaching_analytics"
private const val SUBTYPE_COACHING_APPOINTMENTS = "coaching_appointments"

private const val SYNC_KEY_COACHING_ACCOUNT_LISTS = "coaching_account_lists"
private const val SYNC_KEY_COACHING_ANALYTICS = "coaching_analytics"
private const val SYNC_KEY_COACHING_APPOINTMENTS = "coaching_appointments"

private const val STALE_DURATION_ACCOUNT_LISTS = DAY_IN_MS
private const val STALE_DURATION_ANALYTICS_CURRENT = HOUR_IN_MS
private const val STALE_DURATION_ANALYTICS_OLD = WEEK_IN_MS
private const val STALE_DURATION_APPOINTMENTS = DAY_IN_MS

private const val ARG_FROM = "from"
private const val ARG_TO = "to"

@Singleton
class CoachingSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val coachingApi: CoachingApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_COACHING_ACCOUNT_LISTS -> syncAccountLists(args)
            SUBTYPE_COACHING_ANALYTICS -> syncAnalytics(args)
            SUBTYPE_COACHING_APPOINTMENTS -> syncAppointmentResults(args)
        }
    }

    // region Coaching Account Lists sync
    private val accountListParams = JsonApiParams().include(CoachingAccountList.JSON_ACCOUNT_LIST)

    private val accountListsMutex = Mutex()
    private suspend fun syncAccountLists(args: Bundle): Unit = accountListsMutex.withLock {
        val lastSyncTime = getLastSyncTime(SYNC_KEY_COACHING_ACCOUNT_LISTS)
            .also { if (!it.needsSync(STALE_DURATION_ACCOUNT_LISTS, args.isForced())) return }

        lastSyncTime.trackSync()
        try {
            val responses = fetchPages { page ->
                coachingApi.getCoachingAccountLists(JsonApiParams().addAll(accountListParams).page(page))
            }

            realmTransaction {
                val accountLists = responses.aggregateData().mapNotNull { it.accountList }
                accountLists.forEach { it.isCoachingAccount = true }

                val existing = getCoachingAccountLists().asExisting()
                saveInRealm(accountLists, existing, deleteOrphanedExistingItems = false)

                if (!responses.hasErrors()) {
                    // XXX: This will orphan models in Realm, but we don't have a clean way to handle that yet.
                    existing.values.forEach { it.isCoachingAccount = false }
                    copyToRealmOrUpdate(lastSyncTime)
                }
            }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error retrieving the coaching account lists from the API")
        }
    }

    @JvmOverloads
    fun syncAccountLists(force: Boolean = false) = Bundle().toSyncTask(SUBTYPE_COACHING_ACCOUNT_LISTS, force)
    // endregion Coaching Account Lists sync

    // region Coaching Analytics sync

    private val analyticsMutex = MutexMap()
    private suspend fun syncAnalytics(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return
        val from = args.getFrom() ?: return
        val to = args.getTo() ?: return
        analyticsMutex.withLock(Triple(accountListId, from, to)) {
            val lastSyncTime =
                getLastSyncTime(SYNC_KEY_COACHING_ANALYTICS, accountListId, from.toString(), to.toString())
                    .also { if (!it.needsSync(analyticsStaleDuration(it, to), args.isForced())) return }

            val params = JsonApiParams()
                .filter(JSON_FILTER_DATE_RANGE, (from..to).asApiDateRange())

            lastSyncTime.trackSync()
            try {
                coachingApi.getCoachingAnalytics(accountListId, params)
                    .onSuccess { body ->
                        val analytics = body.dataSingle ?: return@onSuccess
                        analytics.accountListId = accountListId
                        analytics.startDate = from
                        analytics.endDate = to

                        realmTransaction {
                            saveInRealm(analytics)
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving coaching analytics from the API")
            }
        }
    }

    private fun analyticsStaleDuration(lastSync: LastSyncTime, date: LocalDate) = when {
        lastSync.lastSync?.toBpInstant()?.isBefore(date.plusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
            ?: true -> STALE_DURATION_ANALYTICS_CURRENT
        else -> STALE_DURATION_ANALYTICS_OLD
    }

    fun syncAnalytics(accountListId: String?, range: ClosedRange<LocalDate>, force: Boolean = false) =
        syncAnalytics(accountListId, range.start, range.endInclusive, force)

    fun syncAnalytics(accountListId: String?, from: LocalDate, to: LocalDate, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .putFrom(from).putTo(to)
        .toSyncTask(SUBTYPE_COACHING_ANALYTICS, force)

    // endregion Coaching Analytics sync

    // region Coaching Appointment Results sync

    private val appointmentResultsMutex = MutexMap()
    private suspend fun syncAppointmentResults(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return
        val from = args.getFrom() ?: return
        val to = args.getTo()?.takeUnless { it.isBefore(from) } ?: return
        val days = from.until(to, ChronoUnit.DAYS) + 1
        val month = YearMonth.from(from)
        appointmentResultsMutex.withLock(Triple(accountListId, from, to)) {
            val lastSyncTime =
                getLastSyncTime(SYNC_KEY_COACHING_APPOINTMENTS, accountListId, from.toString(), to.toString())
                    .also { if (!it.needsSync(STALE_DURATION_APPOINTMENTS, args.isForced())) return }

            val params = JsonApiParams()
                .filter(
                    JSON_FILTER_APPOINTMENTS_RANGE,
                    when {
                        from == to -> "1d"
                        days == 7L -> "1w"
                        from == month.atDay(1) && to == month.atEndOfMonth() -> "1m"
                        else -> "${days}d"
                    }
                )
                .filter(CoachingAppointmentResults.JSON_END_DATE, to.toString())

            lastSyncTime.trackSync()
            try {
                coachingApi.getCoachingAppointmentResults(accountListId, params)
                    .onSuccess { body ->
                        realmTransaction {
                            saveInRealm(body.data.onEach { it.accountListId = accountListId })
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving coaching appointment results from the API")
            }
        }
    }

    fun syncAppointmentResults(accountListId: String?, range: ClosedRange<LocalDate>, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .putFrom(range.start).putTo(range.endInclusive)
        .toSyncTask(SUBTYPE_COACHING_APPOINTMENTS, force)

    // endregion Coaching Appointment Results sync

    private fun Bundle.putFrom(date: LocalDate) = apply { putSerializable(ARG_FROM, date) }
    private fun Bundle.getFrom(): LocalDate? = getSerializable(ARG_FROM) as? LocalDate
    private fun Bundle.putTo(date: LocalDate) = apply { putSerializable(ARG_TO, date) }
    private fun Bundle.getTo(): LocalDate? = getSerializable(ARG_TO) as? LocalDate
}
