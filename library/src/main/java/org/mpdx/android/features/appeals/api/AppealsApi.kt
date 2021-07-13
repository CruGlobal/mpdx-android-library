package org.mpdx.android.features.appeals.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.FILTER_ACCOUNT_LIST_ID
import org.mpdx.android.features.appeals.model.Appeal
import org.mpdx.android.features.appeals.model.ExcludedAppealContact
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

private const val PATH_APPEALS = "appeals"
internal const val PARAM_APPEAL_ID = "appeal_id"
internal const val PATH_APPEAL = "$PATH_APPEALS/{$PARAM_APPEAL_ID}"

interface AppealsApi {
    // region Appeals
    @GET(PATH_APPEALS)
    suspend fun getAppeals(
        @Query(FILTER_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<Appeal>>

    @GET(PATH_APPEAL)
    suspend fun getAppeal(@Path(PARAM_APPEAL_ID) appealId: String): Response<JsonApiObject<Appeal>>
    // endregion Appeals

    // region ExcludedAppealContact
    @GET("$PATH_APPEAL/excluded_appeal_contacts")
    suspend fun getExcludedAppealContact(
        @Path(PARAM_APPEAL_ID) appealId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<ExcludedAppealContact>>
    // endregion ExcludedAppealContact
}
