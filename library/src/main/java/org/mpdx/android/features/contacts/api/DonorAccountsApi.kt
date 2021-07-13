package org.mpdx.android.features.contacts.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.PARAM_ACCOUNT_LIST_ID
import org.mpdx.android.base.api.ApiConstants.PATH_ACCOUNT_LIST
import org.mpdx.android.features.contacts.model.DonorAccount
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

private const val PATH_DONOR_ACCOUNTS = "$PATH_ACCOUNT_LIST/donor_accounts"

interface DonorAccountsApi {
    @GET(PATH_DONOR_ACCOUNTS)
    suspend fun getDonorAccounts(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<DonorAccount>>
}
