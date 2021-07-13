package org.mpdx.android.features.tasks.sync

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.tasks.api.TasksApi
import org.mpdx.android.features.tasks.model.Comment
import org.mpdx.android.features.tasks.realm.getDirtyComments
import org.mpdx.android.features.tasks.realm.getTask
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "CommentsSyncService"

@VisibleForTesting
internal const val SUBTYPE_COMMENTS_DIRTY = "comments_dirty"

private const val ERROR_BLANK_COMMENT = "Body can't be blank"
private const val ERROR_TASK_NOT_FOUND = "Couldn't find Task with"

@Singleton
class CommentsSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val tasksApi: TasksApi,
    private val tasksSyncService: TasksSyncService
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_COMMENTS_DIRTY -> syncDirtyComments(args)
        }
    }

    // region Dirty Comments sync

    private val dirtyCommentsMutex = Mutex()
    private suspend fun syncDirtyComments(args: Bundle) = dirtyCommentsMutex.withLock {
        coroutineScope {
            realm { getDirtyComments().findAll().copyFromRealm() }
                .forEach { comment: Comment ->
                    launch {
                        if (comment.task?.isNew == true) tasksSyncService.syncDirtyTasks().run()
                        when {
                            comment.isDeleted -> TODO("Deleting comments is not currently supported on Android")
                            comment.isNew -> syncNewComment(comment)
                            comment.hasChangedFields -> syncChangedComment(comment)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewComment(comment: Comment) {
        val taskId = comment.task?.id ?: return
        try {
            comment.prepareForApi()
            tasksApi.createCommentAsync(taskId, comment)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(comment)
                        val task = getTask(taskId).findFirst()
                        saveInRealm(body.data.onEach { it.task = task })
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // blank comment body
                            code() == 400 && error.detail == ERROR_BLANK_COMMENT -> {
                                realmTransaction { deleteObj(comment) }
                                return@onError true
                            }
                            code() == 404 && error.detail?.startsWith(ERROR_TASK_NOT_FOUND) == true -> {
                                if (comment.task?.isNew != true) realmTransaction { deleteObj(comment) }
                                return@onError true
                            }
                        }
                        Timber.tag(TAG).e("Unrecognized JsonApiError %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error syncing a comment to the API")
        }
    }

    private suspend fun syncChangedComment(comment: Comment) {
        val taskId = comment.task?.id ?: return
        val commentId = comment.id ?: return
        try {
            tasksApi.updateComment(taskId, commentId, createPartialUpdate(comment)).onSuccess { body ->
                realmTransaction {
                    clearChangedFields(comment)
                    val task = getTask(taskId).findFirst()
                    saveInRealm(body.data.onEach { it.task = task })
                }
            }.onError { data ->
                if (code() == 500) return@onError true

                data?.errors?.forEach { error ->
                    Timber.tag(TAG).e("Unhandled JsonApiError: %s", error.detail)
                }
                return@onError false
            }
        } catch (e: IOException) {
            Timber.tag(TAG).d("Error syncing the changed Comment %s to the API", commentId)
        }
    }

    fun syncDirtyComments() = Bundle().toSyncTask(SUBTYPE_COMMENTS_DIRTY)

    // endregion Dirty Comments sync
}
