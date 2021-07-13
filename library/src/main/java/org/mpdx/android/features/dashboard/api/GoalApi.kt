package org.mpdx.android.features.dashboard.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.FILTER_ACCOUNT_LIST_ID
import org.mpdx.android.features.dashboard.model.GoalProgress
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

private const val PATH_GOAL_PROGRESS = "reports/goal_progress"

interface GoalApi {
    @GET(PATH_GOAL_PROGRESS)
    suspend fun getGoalProgressAsync(
        @Query(FILTER_ACCOUNT_LIST_ID) accountListId: String
    ): Response<JsonApiObject<GoalProgress>>
}
