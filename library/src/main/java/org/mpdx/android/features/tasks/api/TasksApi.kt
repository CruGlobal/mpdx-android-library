package org.mpdx.android.features.tasks.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.ccci.gto.android.common.jsonapi.retrofit2.annotation.JsonApiInclude
import org.mpdx.android.base.api.ApiConstants.FILTER_ACCOUNT_LIST_ID
import org.mpdx.android.features.tasks.model.Comment
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.model.TaskAnalytics
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

private const val PARAM_TASK_ID = "task_id"
private const val PARAM_COMMENT_ID = "comment_id"
const val PATH_TASKS = "tasks"
private const val PATH_TASK = "$PATH_TASKS/{$PARAM_TASK_ID}"
private const val PATH_COMMENTS = "$PATH_TASK/comments"
private const val PATH_COMMENT = "$PATH_COMMENTS/{$PARAM_COMMENT_ID}"
private const val PATH_TASK_ANALYTICS = "$PATH_TASKS/analytics"

interface TasksApi {
    // region Tasks
    @GET(PATH_TASKS)
    suspend fun getTasks(
        @Query(FILTER_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<Task>>

    @GET(PATH_TASK)
    suspend fun getTask(
        @Path(PARAM_TASK_ID) taskId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<Task>>

    @POST(PATH_TASKS)
    suspend fun createTask(
        @QueryMap params: Map<String, String>,
        @Body @JsonApiInclude(Task.JSON_TASK_CONTACTS) task: Task
    ): Response<JsonApiObject<Task>>

    @PUT(PATH_TASK)
    suspend fun updateTask(
        @Path(PARAM_TASK_ID) taskId: String,
        @QueryMap params: Map<String, String>,
        @Body task: JsonApiObject<Task>
    ): Response<JsonApiObject<Task>>

    @DELETE(PATH_TASK)
    suspend fun deleteTask(@Path(PARAM_TASK_ID) taskId: String): Response<JsonApiObject<Task>>
    // endregion Tasks

    // region Comments
    @POST(PATH_COMMENTS)
    suspend fun createCommentAsync(
        @Path(PARAM_TASK_ID) taskId: String,
        @Body comment: Comment
    ): Response<JsonApiObject<Comment>>

    @PUT(PATH_COMMENT)
    suspend fun updateComment(
        @Path(PARAM_TASK_ID) taskId: String,
        @Path(PARAM_COMMENT_ID) commentId: String,
        @Body comment: JsonApiObject<Comment>
    ): Response<JsonApiObject<Comment>>
    // endregion Comments

    // region Analytics

    @GET(PATH_TASK_ANALYTICS)
    suspend fun getTaskAnalyticsAsync(
        @Query(FILTER_ACCOUNT_LIST_ID) accountListId: String
    ): Response<JsonApiObject<TaskAnalytics>>

    // endregion Analytics
}
