package org.mpdx.android.features.appeals.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.PARAM_ACCOUNT_LIST_ID
import org.mpdx.android.base.api.ApiConstants.PATH_ACCOUNT_LIST
import org.mpdx.android.features.appeals.model.Pledge
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.QueryMap

private const val PATH_PLEDGES = "$PATH_ACCOUNT_LIST/pledges"
private const val PARAM_PLEDGE = "pledge_id"
private const val PATH_PLEDGE = "$PATH_PLEDGES/{$PARAM_PLEDGE}"

interface PledgesApi {
    @GET(PATH_PLEDGES)
    suspend fun getPledges(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<Pledge>>

    @POST(PATH_PLEDGES)
    suspend fun createPledge(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @Body pledge: Pledge
    ): Response<JsonApiObject<Pledge>>

    @PUT(PATH_PLEDGE)
    suspend fun updatePledge(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @Path(PARAM_PLEDGE) pledgeId: String,
        @Body pledge: JsonApiObject<Pledge>
    ): Response<JsonApiObject<Pledge>>

    @DELETE(PATH_PLEDGE)
    suspend fun deletePledge(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @Path(PARAM_PLEDGE) pledgeId: String
    ): Response<JsonApiObject<Pledge>>
}
