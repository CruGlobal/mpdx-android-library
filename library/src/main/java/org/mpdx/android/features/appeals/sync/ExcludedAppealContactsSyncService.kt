package org.mpdx.android.features.appeals.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import org.ccci.gto.android.common.base.TimeConstants.DAY_IN_MS
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.api.util.perPage
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.appeals.api.AppealsApi
import org.mpdx.android.features.appeals.realm.getAppeal
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "XcludedAppealContctSync"

private const val SUBTYPE_EXCLUDED_APPEAL_CONTACTS = "excluded_appeal_contacts"

private const val SYNC_EXCLUDED_APPEAL_CONTACT = "excluded_appeal_contacts"
private const val STALE_DURATION_EXCLUDED_APPEAL_CONTACT = DAY_IN_MS

@Singleton
class ExcludedAppealContactsSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    appealsApi: AppealsApi
) : BaseAppealsSyncService(syncDispatcher, jsonApiConverter, appealsApi) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_EXCLUDED_APPEAL_CONTACTS -> syncExcludedContacts(args)
        }
    }

    private val excludedAppealContactMutex = MutexMap()
    private suspend fun syncExcludedContacts(args: Bundle) {
        val appealId = args.getAppealId() ?: return

        excludedAppealContactMutex.withLock(appealId) {
            val lastSyncTime = getLastSyncTime(SYNC_EXCLUDED_APPEAL_CONTACT, appealId)
                .also { if (!it.needsSync(STALE_DURATION_EXCLUDED_APPEAL_CONTACT, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                val response = fetchPages { page ->
                    appealsApi.getExcludedAppealContact(
                        appealId = appealId,
                        params = JsonApiParams().page(page).perPage(500)
                    )
                }
                realmTransaction {
                    saveInRealm(
                        response.aggregateData(),
                        getAppeal(appealId).findFirst()?.excludedContacts?.asExisting(),
                        !response.hasErrors()
                    )
                    if (!response.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
                }
            } catch (e: IOException) {
                Timber.tag(TAG)
                    .d(e, "Error retrieving the excluded contacts for appeal %s from the API", appealId)
            }
        }
    }

    fun syncExcludedContactsFor(appealId: String, force: Boolean = false) =
        Bundle().putAppealId(appealId).toSyncTask(SUBTYPE_EXCLUDED_APPEAL_CONTACTS, force)
}
