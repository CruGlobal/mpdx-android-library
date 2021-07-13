package org.mpdx.android.core.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import org.ccci.gto.android.common.base.TimeConstants.DAY_IN_MS
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.model.addUnique
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.core.TagsApi
import org.mpdx.android.core.model.orPlaceholder
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.core.realm.getContactTagsFor
import org.mpdx.android.core.realm.getTaskTagsFor
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "TagsSyncService"

private const val SUBTYPE_CONTACT_TAGS = "contact"
private const val SUBTYPE_TASK_TAGS = "task"

private const val SYNC_CONTACT_TAGS = "tags_contact"
private const val SYNC_TASK_TAGS = "tags_task"

private const val STALE_DURATION_CONTACT_TAGS = DAY_IN_MS
private const val STALE_DURATION_TASK_TAGS = DAY_IN_MS

@Singleton
class TagsSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val tagsApi: TagsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_CONTACT_TAGS -> syncContactTags(args)
            SUBTYPE_TASK_TAGS -> syncTaskTags(args)
        }
    }

    // region Contact Tags sync

    private val contactTagsMutex = MutexMap()
    private suspend fun syncContactTags(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return

        contactTagsMutex.withLock(accountListId) {
            val lastSyncTime = getLastSyncTime(SYNC_CONTACT_TAGS, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_CONTACT_TAGS, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                tagsApi.getContactTagsAsync(accountListId)
                    .onSuccess { body ->
                        realmTransaction {
                            val accountList = getAccountList(accountListId).findFirst().orPlaceholder(accountListId)
                            body.data.forEach { it.contactTagFor.addUnique(accountList) }

                            val existing = getContactTagsFor(accountListId).asExisting()
                            saveInRealm(body.data, existing, deleteOrphanedExistingItems = false)
                            // XXX: This will orphan models in Realm, but we don't have a clean way to handle this yet.
                            existing.values.forEach { it.contactTagFor.remove(accountList) }

                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the contact tags from the API")
            }
        }
    }

    fun syncContactTags(accountListId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(SUBTYPE_CONTACT_TAGS, force)

    // endregion Contact Tags sync

    // region Task Tags sync

    private val taskTagsMutex = MutexMap()
    private suspend fun syncTaskTags(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return

        taskTagsMutex.withLock(accountListId) {
            val lastSyncTime = getLastSyncTime(SYNC_TASK_TAGS, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_TASK_TAGS, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                tagsApi.getTaskTagsAsync(accountListId)
                    .onSuccess { body ->
                        realmTransaction {
                            val accountList = getAccountList(accountListId).findFirst().orPlaceholder(accountListId)
                            body.data.forEach { it.taskTagFor.addUnique(accountList) }

                            val existing = getTaskTagsFor(accountListId).asExisting()
                            saveInRealm(body.data, existing, deleteOrphanedExistingItems = false)
                            // XXX: This will orphan models in Realm, but we don't have a clean way to handle this yet.
                            existing.values.forEach { it.taskTagFor.remove(accountList) }

                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the task tags from the API")
            }
        }
    }

    fun syncTaskTags(accountListId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(SUBTYPE_TASK_TAGS, force)

    // endregion Task Tags sync
}
