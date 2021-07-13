package org.mpdx.android.features.donations.sync

import android.os.Bundle
import io.realm.Realm
import io.realm.RealmObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.base.TimeConstants
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.ApiConstants.ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX
import org.mpdx.android.base.api.util.asApiDateRange
import org.mpdx.android.base.api.util.filter
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.api.util.perPage
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.core.model.orPlaceholder
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.DonorAccount
import org.mpdx.android.features.contacts.model.JSON_API_TYPE_CONTACT
import org.mpdx.android.features.donations.api.DonationsApi
import org.mpdx.android.features.donations.model.Donation
import org.mpdx.android.features.donations.realm.forAccountList
import org.mpdx.android.features.donations.realm.forMonth
import org.mpdx.android.features.donations.realm.getDonations
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import org.mpdx.android.utils.toLocalDateRange
import org.threeten.bp.YearMonth
import timber.log.Timber

private const val TAG = "DonationsSyncService"

private const val SUBTYPE_DONATIONS = "donations"
private const val SUBTYPE_DONATIONS_FOR_ACCOUNTS = "donations_donor_accounts"
private const val SUBTYPE_DONATIONS_BATCH = "donations_batch"
private const val SUBTYPE_DONATIONS_DIRTY = "donations_dirty"

private const val SYNC_KEY_DONATIONS = "donations"
private const val SYNC_KEY_DONATIONS_FOR_ACCOUNTS = "donations_donor_accounts"

private const val STALE_DURATION_DONATIONS_CURRENT_MONTH = 12 * TimeConstants.HOUR_IN_MS
private const val STALE_DURATION_DONATIONS_LAST_MONTH = TimeConstants.DAY_IN_MS
private const val STALE_DURATION_DONATIONS_PAST_YEAR = TimeConstants.WEEK_IN_MS
private const val STALE_DURATION_DONATIONS_OTHER = 30 * TimeConstants.DAY_IN_MS
private const val STALE_DURATION_DONATIONS_FOR_ACCOUNTS = TimeConstants.DAY_IN_MS

private const val ERROR_DONATION_NOT_FOUND = "Couldn't find Donation with 'id'"

private const val ARG_BATCH_SIZE = "batch_size"
private const val ARG_MONTH = "month"
private const val ARG_DONOR_ACCOUNTS = "donor_accounts"

private const val DEFAULT_MONTHS_BATCH = 13
private const val PAGE_SIZE = 100

@Singleton
class DonationsSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val donationsApi: DonationsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_DONATIONS -> syncDonations(args)
            SUBTYPE_DONATIONS_FOR_ACCOUNTS -> syncDonationsForDonorAccounts(args)
            SUBTYPE_DONATIONS_BATCH -> syncDonationsBatch(args)
            SUBTYPE_DONATIONS_DIRTY -> syncDirtyDonations(args)
        }
    }

    // region Donations Batch
    private suspend fun syncDonationsBatch(args: Bundle) = coroutineScope {
        args.getAccountListId()?.let { accountListId ->
            val force = args.isForced()
            for (i in 0 until args.getBatchSize(DEFAULT_MONTHS_BATCH)) {
                launch { syncDonations(accountListId, YearMonth.now().minusMonths(i.toLong()), force).run() }
            }
        }
    }

    fun syncDonationsBatch(accountListId: String, months: Int = DEFAULT_MONTHS_BATCH, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .putBatchSize(months)
        .toSyncTask(SUBTYPE_DONATIONS_BATCH, force)
    // endregion Donations Batch

    private val baseParams = JsonApiParams()
        .include("${Donation.JSON_DONOR_ACCOUNT}.${DonorAccount.JSON_CONTACTS}")
        .fields(JSON_API_TYPE_CONTACT, *Contact.JSON_FIELDS_SPARSE)
        .perPage(PAGE_SIZE)

    // region Donations sync
    private val donationsMutex = MutexMap()

    private suspend fun syncDonations(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return
        val month = args.getMonth() ?: return
        donationsMutex.withLock(Pair(accountListId, month)) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_DONATIONS, accountListId, month.toString())
                .also { if (!it.needsSync(donationsStaleDuration(month), args.isForced())) return }

            val params = JsonApiParams().addAll(baseParams)
                .filter(Donation.JSON_DONATION_DATE, month.toLocalDateRange().asApiDateRange())

            lastSyncTime.trackSync()
            try {
                val responses = fetchPages { page ->
                    donationsApi.getDonationsAsync(accountListId, JsonApiParams().addAll(params).page(page))
                }

                realmTransaction {
                    saveInRealm(
                        accountListId, responses.aggregateData(),
                        getDonations().forAccountList(accountListId).forMonth(month).asExisting(),
                        deleteOrphanedExistingItems = !responses.hasErrors()
                    )
                    if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the donations from the API")
            }
        }
    }

    private fun donationsStaleDuration(month: YearMonth) = when {
        month >= YearMonth.now() -> STALE_DURATION_DONATIONS_CURRENT_MONTH
        month >= YearMonth.now().minusMonths(1) -> STALE_DURATION_DONATIONS_LAST_MONTH
        month >= YearMonth.now().minusYears(1) -> STALE_DURATION_DONATIONS_PAST_YEAR
        else -> STALE_DURATION_DONATIONS_OTHER
    }

    fun syncDonations(accountListId: String, month: YearMonth, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .putMonth(month)
        .toSyncTask(SUBTYPE_DONATIONS, force)
    // endregion Donations sync

    // region Donations for Donor Accounts sync
    private val donationsByDonorAccountsMutex = MutexMap()

    private suspend fun syncDonationsForDonorAccounts(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return
        val donorAccounts = args.getDonorAccounts()
            ?.filterNotNull()?.takeUnless { it.isEmpty() }
            ?.distinct()?.sorted()?.joinToString(",")?.toLowerCase() ?: return
        donationsByDonorAccountsMutex.withLock(Pair(accountListId, donorAccounts)) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_DONATIONS_FOR_ACCOUNTS, accountListId, donorAccounts)
                .also { if (!it.needsSync(STALE_DURATION_DONATIONS_FOR_ACCOUNTS, args.isForced())) return }

            val params = JsonApiParams().addAll(baseParams)
                .filter(Donation.JSON_DONOR_ACCOUNT_ID, donorAccounts)

            lastSyncTime.trackSync()
            try {
                val responses = fetchPages { page ->
                    donationsApi.getDonationsAsync(accountListId, JsonApiParams().addAll(params).page(page))
                }

                realmTransaction {
                    saveInRealm(accountListId, responses.aggregateData())
                    copyToRealmOrUpdate(lastSyncTime)
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the donations for donor accounts from the API")
            }
        }
    }

    private fun Bundle.putDonorAccounts(vararg accounts: String) =
        apply { putStringArray(ARG_DONOR_ACCOUNTS, accounts) }
    private fun Bundle.getDonorAccounts() = getStringArray(ARG_DONOR_ACCOUNTS)

    fun syncDonationsForDonorAccounts(accountListId: String, force: Boolean = false, vararg accounts: String) = Bundle()
        .putAccountListId(accountListId)
        .putDonorAccounts(*accounts)
        .toSyncTask(SUBTYPE_DONATIONS_FOR_ACCOUNTS, force)
    // endregion Donations for Donor Accounts sync

    // region Donations Dirty sync
    private val dirtyDonationsMutex = Mutex()
    private suspend fun syncDirtyDonations(args: Bundle) {
        dirtyDonationsMutex.withLock {
            coroutineScope {
                realm { copyFromRealm(getDonations().isDirty().findAll()) }
                    .forEach { donation ->
                        launch {
                            when {
                                donation.isDeleted -> TODO("We currently don't support deleting Donations on Android")
                                donation.isNew -> syncNewDonation(donation)
                                donation.hasChangedFields -> syncChangedDonation(donation)
                            }
                        }
                    }
            }
        }
    }

    private suspend fun syncNewDonation(donation: Donation) {
        val accountListId = donation.accountList?.id ?: return
        try {
            donation.prepareForApi()
            donationsApi.addDonationAsync(accountListId, donation)
                .onSuccess { body ->
                    realmTransaction {
                        val accountList = getAccountList(accountListId).findFirst().orPlaceholder(accountListId)
                        clearNewFlag(donation)
                        saveInRealm(body.data.onEach { it.accountList = accountList })
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonApiError %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error creating a donation on the API %s", donation.id)
        }
    }

    private suspend fun syncChangedDonation(donation: Donation) {
        val accountListId = donation.accountList?.id ?: return
        val donationId = donation.id ?: return
        try {
            donationsApi.updateDonationAsync(accountListId, donationId, createPartialUpdate(donation))
                .onSuccess { body ->
                    realmTransaction {
                        val accountList = getAccountList(accountListId).findFirst().orPlaceholder(accountListId)
                        clearChangedFields(donation)
                        saveInRealm(body.data.onEach { it.accountList = accountList })
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // invalid updated_in_db_at time
                            code() == 409 && error.detail?.startsWith(ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX) == true -> {
                                // TODO: force a sync of this donation once we support syncing individual donations
                                return@onError true
                            }
                            // donation wasn't found to update
                            // NOTE: my guess is that the donation was deleted on the API before the local changes to
                            //       the donation were synced. -DF
                            code() == 404 && error.detail?.startsWith(ERROR_DONATION_NOT_FOUND) == true -> {
                                // TODO: force a sync of this donation once we support syncing individual donations
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error storing the changed donation %s to the API", donationId)
        }
    }

    fun syncDirtyDonations() = Bundle().toSyncTask(SUBTYPE_DONATIONS_DIRTY)
    // endregion Donations Dirty sync

    // region Realm functions
    private fun Realm.saveInRealm(
        accountListId: String,
        donations: Collection<Donation?>,
        existingItems: MutableMap<String?, out RealmObject>? = null,
        deleteOrphanedExistingItems: Boolean = true
    ) {
        val accountList = getAccountList(accountListId).findFirst().orPlaceholder(accountListId)
        saveInRealm(
            donations.onEach { donation ->
                donation?.accountList = accountList
                donation?.donorAccount?.contacts?.onEach {
                    it?.isPlaceholder = true
                    it?.replacePlaceholder = true
                }
            },
            existingItems,
            deleteOrphanedExistingItems
        )
    }
    // endregion Realm functions
}

private fun Bundle.putBatchSize(size: Int) = apply { putInt(ARG_BATCH_SIZE, size) }
private fun Bundle.getBatchSize(default: Int = 0) = getInt(ARG_BATCH_SIZE, default)
private fun Bundle.putMonth(month: YearMonth) = apply { putSerializable(ARG_MONTH, month) }
private fun Bundle.getMonth(): YearMonth? = getSerializable(ARG_MONTH) as? YearMonth
