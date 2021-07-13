package org.mpdx.android.features.tasks.sync

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import dagger.Lazy
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
import org.ccci.gto.android.common.base.TimeConstants.HOUR_IN_MS
import org.ccci.gto.android.common.base.TimeConstants.WEEK_IN_MS
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams
import org.ccci.gto.android.common.kotlin.coroutines.MutexMap
import org.ccci.gto.android.common.kotlin.coroutines.withLock
import org.mpdx.android.base.api.ApiConstants.ERROR_DUPLICATE_UNIQUE_KEY
import org.mpdx.android.base.api.ApiConstants.ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_AT
import org.mpdx.android.base.api.util.asApiTimeRange
import org.mpdx.android.base.api.util.filter
import org.mpdx.android.base.api.util.page
import org.mpdx.android.base.api.util.perPage
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.core.DeletedRecordsApi
import org.mpdx.android.core.model.DeletedRecord
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.JSON_API_TYPE_CONTACT
import org.mpdx.android.features.contacts.sync.ContactsSyncService
import org.mpdx.android.features.tasks.api.TasksApi
import org.mpdx.android.features.tasks.model.Comment
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.model.Task.Companion.JSON_FILTER_CONTACT_IDS
import org.mpdx.android.features.tasks.model.TaskContact
import org.mpdx.android.features.tasks.realm.completed
import org.mpdx.android.features.tasks.realm.forContact
import org.mpdx.android.features.tasks.realm.getDirtyTasks
import org.mpdx.android.features.tasks.realm.getTask
import org.mpdx.android.features.tasks.realm.getTasks
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import org.mpdx.android.utils.withoutFractionalSeconds
import org.threeten.bp.Instant
import timber.log.Timber

private const val TAG = "TasksSyncService"

@VisibleForTesting
internal const val SUBTYPE_SINGLE_TASK = "task"
private const val SUBTYPE_TASKS = "tasks"
private const val SUBTYPE_TASKS_COMPLETED = "tasks_completed"
private const val SUBTYPE_TASKS_CONTACT = "tasks_contact"
private const val SUBTYPE_TASKS_DELETED = "tasks_deleted"
@VisibleForTesting
internal const val SUBTYPE_TASKS_DIRTY = "tasks_dirty"
private const val SUBTYPE_TASK_ANALYTICS = "task_analytics"

private const val SYNC_KEY_SINGLE_TASK = "task"
private const val SYNC_KEY_TASKS = "tasks"
private const val SYNC_KEY_TASKS_COMPLETED = "tasks_completed"
private const val SYNC_KEY_TASKS_CONTACT = "tasks_contact"
private const val SYNC_KEY_TASKS_DELETED = "tasks_deleted"
private const val SYNC_KEY_TASK_ANALYTICS = "task_analytics"

private const val STALE_DURATION_SINGLE_TASK = 6 * HOUR_IN_MS
private const val STALE_DURATION_TASKS = 6 * HOUR_IN_MS
private const val STALE_DURATION_TASKS_FULL = 4 * WEEK_IN_MS
private const val STALE_DURATION_TASKS_COMPLETED = DAY_IN_MS
private const val STALE_DURATION_TASKS_CONTACT = DAY_IN_MS
private const val STALE_DURATION_TASKS_DELETED = 6 * HOUR_IN_MS
private const val STALE_DURATION_TASK_ANALYTICS = 6 * HOUR_IN_MS

@VisibleForTesting
internal const val ARG_TASK = "task"

private const val ERROR_CONTACT_NOT_FOUND_PREFIX = "Couldn't find Contact with 'id'="
private const val ERROR_KEY_PRIMARY = "activities_pkey"
private const val ERROR_TASK_NOT_FOUND = "Couldn't find Task with 'id'="
private const val ERROR_TASK_SUBJECT_BLANK = "Subject can't be blank"

@Singleton
class TasksSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val deletedRecordsApi: DeletedRecordsApi,
    private val tasksApi: TasksApi,
    private val contactsSyncService: Lazy<ContactsSyncService>
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    companion object {
        const val PAGE_SIZE = 100
    }

    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_SINGLE_TASK -> syncSingleTask(args)
            SUBTYPE_TASKS -> syncTasks(args)
            SUBTYPE_TASKS_CONTACT -> syncTasksForContact(args)
            SUBTYPE_TASKS_COMPLETED -> syncCompletedTasks(args)
            SUBTYPE_TASKS_DELETED -> syncDeletedTasks(args)
            SUBTYPE_TASKS_DIRTY -> syncDirtyTasks(args)
            SUBTYPE_TASK_ANALYTICS -> syncTaskAnalytics(args)
        }
    }

    private val baseTaskParams = JsonApiParams()
        .include(Task.JSON_USER)
        .include("${Task.JSON_TASK_CONTACTS}.${TaskContact.JSON_CONTACT}")
        .include("${Task.JSON_COMMENTS}.${Comment.JSON_PERSON}")
        .fields(JSON_API_TYPE_CONTACT, *Contact.JSON_FIELDS_SPARSE)
        .perPage(PAGE_SIZE)

    // region Single Task sync
    private val taskMutex = MutexMap()

    private suspend fun syncSingleTask(args: Bundle) {
        val taskId = args.getTaskId() ?: return

        taskMutex.withLock(taskId) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_SINGLE_TASK, taskId)
                .also { if (!it.needsSync(STALE_DURATION_SINGLE_TASK, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                tasksApi.getTask(taskId, baseTaskParams)
                    .onSuccess { body ->
                        realmTransaction {
                            saveInRealm(body.data)
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
                    .onError {
                        when (code()) {
                            500 -> true
                            403, 404 -> {
                                realmTransaction { deleteOrphanedItems(getTask(taskId).findAll()) }
                                true
                            }
                            else -> false
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the tasks from the API")
            }
        }
    }

    private fun Bundle.putTaskId(taskId: String?) = apply { putString(ARG_TASK, taskId) }
    private fun Bundle.getTaskId() = getString(ARG_TASK)

    @JvmOverloads
    fun syncTask(taskId: String?, force: Boolean = false) = Bundle()
        .putTaskId(taskId)
        .toSyncTask(SUBTYPE_SINGLE_TASK, force)
    // endregion Single Task sync

    // region Tasks sync

    private val tasksMutex = MutexMap()

    private suspend fun syncTasks(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return

        tasksMutex.withLock(accountListId) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_TASKS, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_TASKS, args.isForced())) return }
            val needsFullSync = lastSyncTime.needsFullSync(STALE_DURATION_TASKS_FULL, args.isForced())

            // build sync params
            val params = JsonApiParams().addAll(baseTaskParams)
            if (needsFullSync) {
                // we only filter for incomplete tasks when doing a full sync
                params.filter(Task.JSON_COMPLETED, "false")
            } else {
                params.filter(JSON_ATTR_UPDATED_AT, lastSyncTime.getSinceLastSyncRange().asApiTimeRange())
            }

            lastSyncTime.trackSync(fullSync = needsFullSync)
            try {
                val responses = fetchPages { page ->
                    tasksApi.getTasks(accountListId, JsonApiParams().addAll(params).page(page))
                }

                realmTransaction {
                    val existing = if (needsFullSync) {
                        getTasks(accountListId, includeDeleted = true).completed(false).asExisting()
                    } else {
                        mutableMapOf()
                    }

                    saveInRealm(
                        responses.aggregateData(), existing,
                        // this could delete tasks that have been updated locally but not synced yet...
                        deleteOrphanedExistingItems = false
                    )
                    if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)

                    // force a single task sync for any orphaned tasks
                    existing.keys.filterNotNull().forEach { syncTask(it, true).launch() }
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the tasks from the API")
            }
        }
    }

    fun syncTasks(accountListId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(SUBTYPE_TASKS, force)

    // endregion Tasks sync

    // region Contact Tasks sync
    private val contactTasksMutex = MutexMap()

    private suspend fun syncTasksForContact(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return
        val contactId = args.getContactId() ?: return

        contactTasksMutex.withLock(Pair(accountListId, contactId)) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_TASKS_CONTACT, accountListId, contactId)
                .also { if (!it.needsSync(STALE_DURATION_TASKS_CONTACT, args.isForced())) return }

            val params = JsonApiParams().addAll(baseTaskParams)
                .filter(JSON_FILTER_CONTACT_IDS, contactId)

            lastSyncTime.trackSync()
            try {
                val responses = fetchPages { page ->
                    tasksApi.getTasks(accountListId, JsonApiParams().addAll(params).page(page))
                }

                realmTransaction {
                    saveInRealm(
                        responses.aggregateData(),
                        getTasks(accountListId).forContact(contactId).asExisting(),
                        deleteOrphanedExistingItems = !responses.hasErrors()
                    )
                    if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
                }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the tasks for a contact from the API")
            }
        }
    }

    fun syncTasksForContact(accountListId: String, contactId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .putContactId(contactId)
        .toSyncTask(SUBTYPE_TASKS_CONTACT, force)
    // endregion Contact Tasks sync

    // region Completed Tasks sync

    private val completedTasksMutex = MutexMap()
    private suspend fun syncCompletedTasks(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return
        val page = args.getPage().takeIf { it >= 1 } ?: return

        completedTasksMutex.getMutex(Pair(accountListId, page)).withLock {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_TASKS_COMPLETED, accountListId, page.toString())
                .also { if (!it.needsSync(STALE_DURATION_TASKS_COMPLETED, args.isForced())) return }

            val params = JsonApiParams().addAll(baseTaskParams)
                .filter(Task.JSON_COMPLETED, "true")
                .page(page)

            lastSyncTime.trackSync()
            try {
                tasksApi.getTasks(accountListId, params)
                    .onSuccess { body ->
                        realmTransaction {
                            saveInRealm(body.data)
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving page %d of completed tasks from the API", page)
            }
        }
    }

    fun syncCompletedTasks(accountListId: String, page: Int, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .putPage(page)
        .toSyncTask(SUBTYPE_TASKS_COMPLETED, force)

    // endregion Completed Tasks sync

    // region Deleted Tasks sync

    private val deletedTasksMutex = Mutex()
    private suspend fun syncDeletedTasks(args: Bundle): Unit = deletedTasksMutex.withLock {
        val lastSyncTime = getLastSyncTime(SYNC_KEY_TASKS_DELETED)
            .also { if (!it.needsSync(STALE_DURATION_TASKS_DELETED, args.isForced())) return }
        val since = lastSyncTime.getSinceLastSyncRange(defaultStart = Instant.now().minusMillis(12 * WEEK_IN_MS)).start
            .withoutFractionalSeconds()

        val params = JsonApiParams()
            .perPage(500)

        lastSyncTime.trackSync()
        try {
            val responses = fetchPages { page ->
                deletedRecordsApi.getDeletedRecords(
                    DeletedRecord.TYPE_TASK, since,
                    JsonApiParams().addAll(params).page(page)
                )
            }

            realmTransaction {
                responses.aggregateData().forEach {
                    deleteOrphanedItems(getTask(it.deletableId).findAll())
                }
                if (!responses.hasErrors()) copyToRealmOrUpdate(lastSyncTime)
            }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error retrieving the task deleted records from the API")
        }
    }

    fun syncDeletedTasks(force: Boolean = false) = Bundle().toSyncTask(SUBTYPE_TASKS_DELETED, force)

    // endregion Deleted Tasks sync

    // region Dirty Task sync
    private val dirtyTasksMutex = Mutex()
    private suspend fun syncDirtyTasks(args: Bundle) = dirtyTasksMutex.withLock {
        coroutineScope {
            realm {
                getDirtyTasks().findAll().map { task ->
                    task.copyFromRealm(1).apply {
                        apiTaskContacts = task.taskContacts?.copyFromRealm(1)
                    }
                }
            }
                .forEach { task: Task ->
                    launch {
                        when {
                            task.isDeleted -> syncDeletedTask(task)
                            task.isNew -> syncNewTask(task)
                            else -> {
                                val newContacts = task.apiTaskContacts?.any { it.isNew } == true
                                val deletedContacts = task.apiTaskContacts?.any { it.isDeleted } == true
                                if (task.hasChangedFields || newContacts || deletedContacts) {
                                    syncChangedTask(task, newContacts, deletedContacts)
                                }
                            }
                        }
                    }
                }
        }
    }

    private suspend fun syncNewTask(task: Task) {
        val contacts = task.apiTaskContacts

        try {
            task.prepareForApi()
            task.apiTaskContacts?.forEach { it.prepareForApi() }
            tasksApi.createTask(baseTaskParams, task)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(task)
                        clearChangedTaskContacts(contacts)
                        saveInRealm(body.data)
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // task already exists
                            code() == 409 && error.detail?.run {
                                contains(ERROR_DUPLICATE_UNIQUE_KEY) && contains(ERROR_KEY_PRIMARY)
                            } == true -> {
                                syncTask(task.id, true).run()
                                return@onError true
                            }

                            // contact not found
                            code() == 404 && error.detail?.startsWith(ERROR_CONTACT_NOT_FOUND_PREFIX) == true -> {
                                // force a sync of all attached contacts
                                coroutineScope {
                                    contacts?.mapNotNull { it.contact?.id }?.forEach {
                                        launch { contactsSyncService.get().syncContact(it, true).run() }
                                    }
                                }
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unhandled JsonApiError %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error syncing a task to the API")
        }
    }

    private suspend fun syncChangedTask(task: Task, newContacts: Boolean, deletedContacts: Boolean) {
        val taskId = task.id ?: return
        val contacts = task.apiTaskContacts?.filter { !deletedContacts || it.isDeleted }

        try {
            val partialUpdateModel = createPartialUpdate(task) {
                // HACK: We need to send any pending deleted TaskContacts before adding new Contacts to a task.
                //       This is required because if a contact is deleted and re-added within the same API request the
                //       API will just delete the contact assignment and we will lose data.
                if (newContacts && !deletedContacts) fields(task.jsonApiType, Task.JSON_CONTACTS)
                if (deletedContacts) {
                    fields(task.jsonApiType, Task.JSON_TASK_CONTACTS)
                    include(Task.JSON_TASK_CONTACTS)
                    task.apiTaskContacts?.forEach { it.prepareForApi() }
                }
            }
            tasksApi.updateTask(taskId, baseTaskParams, partialUpdateModel)
                .onSuccess { body ->
                    realmTransaction {
                        if (newContacts || deletedContacts) clearChangedTaskContacts(contacts)
                        clearChangedFields(task)
                        saveInRealm(body.data)
                    }
                }
                .onError { data ->
                    if (code() == 500 || code() == 502) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // task missing subject
                            code() == 400 && error.detail == ERROR_TASK_SUBJECT_BLANK -> {
                                if (task.changedFields.contains(Task.JSON_SUBJECT)) {
                                    // reset the subject to what the API is tracking for it
                                    realmTransaction { getManagedVersion(task)?.clearChanged(Task.JSON_SUBJECT) }
                                    syncTask(taskId, true).run()
                                    return@onError true
                                }

                                // this is unexpected
                                Timber.tag(TAG)
                                    .wtf("Encountered subject error when not trying to update the subject")
                            }
                            // task not found
                            code() == 404 && error.detail?.startsWith(ERROR_TASK_NOT_FOUND) == true -> {
                                syncTask(taskId, true).run()
                                return@onError true
                            }
                            // invalid updated_in_db_at time
                            code() == 409 && error.detail?.startsWith(ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX) == true -> {
                                syncTask(taskId, true).run()
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unhandled JsonApiError: %s", error.detail)
                    }

                    Timber.tag(TAG).e("Task Id: %s", taskId)
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error storing the changed task %s to the API", taskId)
        }
    }

    private suspend fun syncDeletedTask(task: Task) {
        val taskId = task.id ?: return
        try {
            tasksApi.deleteTask(taskId)
                .onSuccess(requireBody = false) { realmTransaction { deleteObj(task) } }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // task not found
                            code() == 404 && error.detail?.startsWith(ERROR_TASK_NOT_FOUND) == true -> {
                                // task was already deleted, so let's delete our local copy
                                realmTransaction { deleteObj(task) }
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error deleting a task")
        }
    }

    fun syncDirtyTasks() = Bundle().toSyncTask(SUBTYPE_TASKS_DIRTY)
    // endregion Dirty Task sync

    // region Task Analytics sync

    private val taskAnalyticsMutex = MutexMap()

    private suspend fun syncTaskAnalytics(args: Bundle) {
        val accountListId = args.getAccountListId() ?: return

        taskAnalyticsMutex.withLock(accountListId) {
            val lastSyncTime = getLastSyncTime(SYNC_KEY_TASK_ANALYTICS, accountListId)
                .also { if (!it.needsSync(STALE_DURATION_TASK_ANALYTICS, args.isForced())) return }

            lastSyncTime.trackSync()
            try {
                tasksApi.getTaskAnalyticsAsync(accountListId)
                    .onSuccess { body ->
                        val analytics = body.dataSingle?.also { it.accountListId = accountListId } ?: return
                        realmTransaction {
                            saveInRealm(analytics)
                            copyToRealmOrUpdate(lastSyncTime)
                        }
                    }
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "Error retrieving the tasks from the API")
            }
        }
    }

    fun syncTaskAnalytics(accountListId: String, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(SUBTYPE_TASK_ANALYTICS, force)

    // endregion Task Analytics sync

    // region Realm Functions
    private fun Realm.saveInRealm(
        tasks: Collection<Task?>,
        existingItems: MutableMap<String?, out RealmObject>? = null,
        deleteOrphanedExistingItems: Boolean = true
    ) {
        tasks.flatMap { it?.apiTaskContacts.orEmpty() }.forEach {
            it.contact?.apply {
                isPlaceholder = true
                replacePlaceholder = true
            }
        }

        saveInRealm(tasks as Collection<RealmObject?>, existingItems, deleteOrphanedExistingItems)
        tasks.filterNotNull().forEach {
            val managed = getManagedVersion(it)

            // save task contacts in Realm
            val existingContacts = managed?.taskContacts?.asExisting()
            saveInRealm(it.apiTaskContacts.orEmpty(), existingContacts)

            // save comments in Realm
            val existingComments = managed?.getComments(includeDeleted = true)?.asExisting()
            saveInRealm(it.apiComments.orEmpty(), existingComments)
        }
    }

    private fun Realm.clearChangedTaskContacts(contacts: List<TaskContact>?) {
        contacts?.forEach {
            when {
                it.isDeleted -> deleteObj(it)
                it.isNew -> clearNewFlag(it)
            }
        }
    }
    // endregion Realm Functions
}
