package org.mpdx.android.features.appeals.sync

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
import org.ccci.gto.android.common.base.TimeConstants.DAY_IN_MS
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.util.filter
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.api.util.perPage
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.appeals.api.AppealsApi
import org.mpdx.android.features.appeals.api.PledgesApi
import org.mpdx.android.features.appeals.model.Pledge
import org.mpdx.android.features.appeals.realm.forAppeal
import org.mpdx.android.features.appeals.realm.getPledges
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "PledgesSyncService"

private const val SUBTYPE_PLEDGES = "pledges"
private const val SUBTYPE_DIRTY_PLEDGES = "dirty_pledges"

private const val SYNC_PLEDGES = "pledges"

private const val STALE_DURATION_PLEDGES = DAY_IN_MS

private const val PAGE_SIZE = 100

@Singleton
class PledgesSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    appealsApi: AppealsApi,
    private val pledgesApi: PledgesApi
) : BaseAppealsSyncService(syncDispatcher, jsonApiConverter, appealsApi) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_PLEDGES -> syncPledges(args)
            SUBTYPE_DIRTY_PLEDGES -> syncDirtyPledges(args)
        }
    }

    private inline val pledgeParams get() = JsonApiParams()
        .include(Pledge.JSON_DONATIONS)
        .perPage(PAGE_SIZE)

    // region Pledges sync
    private val pledgesMutex = MutexMap()
    private suspend fun syncPledges(args: Bundle) {
        val appealId = args.getAppealId() ?: return
        val accountListId = args.getAccountListId() ?: return
        pledgesMutex.withLock(Pair(accountListId, appealId)) {
            val lastSyncTime = getLastSyncTime(SYNC_PLEDGES, appealId)
                .also { if (!it.needsSync(STALE_DURATION_PLEDGES, args.isForced())) return }

            val params = pledgeParams
                .filter(Pledge.JSON_FILTER_APPEAL_ID, appealId)

            lastSyncTime.trackSync()
            try {
                val responses = fetchPages { page ->
                    pledgesApi.getPledges(accountListId, JsonApiParams().addAll(params).page(page))
                }
                realmTransaction {
                    saveInRealm(
                        responses.aggregateData(), getPledges(includeDeleted = true).forAppeal(appealId).asExisting(),
                        deleteOrphanedExistingItems = !responses.hasErrors()
                    )
                    if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
                }
            } catch (e: Exception) {
                Timber.tag(TAG)
                    .d(e, "Error retrieving pledges %s", appealId)
            }
        }
    }

    fun syncPledges(accountListId: String, appealId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .putAppealId(appealId)
        .toSyncTask(SUBTYPE_PLEDGES, force)
    // endregion Pledges sync

    // region Dirty Pledges sync
    private val dirtyPledgesMutex = Mutex()
    private suspend fun syncDirtyPledges(args: Bundle) {
        dirtyPledgesMutex.withLock {
            coroutineScope {
                realm { getPledges(includeDeleted = true).isDirty().findAll().copyFromRealm() }
                    .forEach { pledge ->
                        launch {
                            when {
                                pledge.isDeleted -> syncDeletedPledge(pledge)
                                pledge.isNew -> syncNewPledge(pledge)
                                pledge.hasChangedFields -> syncUpdatedPledge(pledge)
                            }
                        }
                    }
            }
        }
    }

    private suspend fun syncNewPledge(pledge: Pledge) {
        val accountListId = pledge.accountList?.id ?: return

        pledge.prepareForApi()
        try {
            pledgesApi.createPledge(accountListId, pledge)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(pledge)
                        saveInRealm(body.data)
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
                .d(e, "Error adding new pledge")
        }
    }

    private suspend fun syncUpdatedPledge(pledge: Pledge) {
        val accountListId = pledge.accountList?.id ?: return
        val pledgeId = pledge.id ?: return

        try {
            pledgesApi.updatePledge(accountListId, pledgeId, createPartialUpdate(pledge))
                .onSuccess { body ->
                    realmTransaction {
                        clearChangedFields(pledge)
                        saveInRealm(body.data)
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true
                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonapiError %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error updating a Pledge")
        }
    }

    private suspend fun syncDeletedPledge(pledge: Pledge) {
        val accountListId = pledge.accountList?.id ?: return
        val pledgeId = pledge.id ?: return

        try {
            pledgesApi.deletePledge(accountListId, pledgeId)
                .onSuccess(requireBody = false) { realmTransaction { deleteObj(pledge) } }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            error.detail?.startsWith("Couldn't find Pledge with 'id'=") == true -> {
                                // pledge was already deleted, so let's delete our local copy
                                realmTransaction { deleteObj(pledge) }
                                return@onError true
                            }
                        }
                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error deleting a pledge")
        }
    }

    fun syncDirtyPledges() = Bundle().toSyncTask(SUBTYPE_DIRTY_PLEDGES)
    // endregion Dirty Pledges sync

    // region Realm Functions
    private fun Realm.saveInRealm(
        pledges: Collection<Pledge?>,
        existingItems: MutableMap<String?, out RealmObject>? = null,
        deleteOrphanedExistingItems: Boolean = true
    ) {
        saveInRealm(pledges as Collection<RealmObject?>, existingItems, deleteOrphanedExistingItems)
        saveInRealm(pledges.filterNotNull().flatMap { it.apiDonations.orEmpty() })
    }
    // endregion Realm Functions
}
