package org.mpdx.android.features.coaching.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.FILTER_ACCOUNT_LIST_ID
import org.mpdx.android.base.api.ApiConstants.PARAM_ACCOUNT_LIST_ID
import org.mpdx.android.base.api.ApiConstants.PATH_ACCOUNT_LIST
import org.mpdx.android.core.PATH_USER
import org.mpdx.android.features.coaching.model.CoachingAccountList
import org.mpdx.android.features.coaching.model.CoachingAnalytics
import org.mpdx.android.features.coaching.model.CoachingAppointmentResults
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

private const val PATH_COACHING_ACCOUNT_LISTS = "$PATH_USER/account_list_coaches"
private const val PATH_COACHING_ANALYTICS = "$PATH_ACCOUNT_LIST/analytics"

const val JSON_FILTER_DATE_RANGE = "date_range"
const val JSON_FILTER_APPOINTMENTS_RANGE = "range"

interface CoachingApi {
    @GET(PATH_COACHING_ACCOUNT_LISTS)
    suspend fun getCoachingAccountLists(
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<CoachingAccountList>>

    @GET(PATH_COACHING_ANALYTICS)
    suspend fun getCoachingAnalytics(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<CoachingAnalytics>>

    @GET("reports/appointment_results")
    suspend fun getCoachingAppointmentResults(
        @Query(FILTER_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<CoachingAppointmentResults>>
}
