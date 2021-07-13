package org.mpdx.android.features.donations.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.PARAM_ACCOUNT_LIST_ID
import org.mpdx.android.base.api.ApiConstants.PATH_ACCOUNT_LIST
import org.mpdx.android.features.donations.model.DesignationAccount
import org.mpdx.android.features.donations.model.Donation
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.QueryMap

private const val PARAM_DONATION_ID = "donation_id"
private const val PATH_DONATIONS = "$PATH_ACCOUNT_LIST/donations"
private const val PATH_DONATION = "$PATH_DONATIONS/{$PARAM_DONATION_ID}"
private const val PATH_DESIGNATION_ACCOUNTS = "$PATH_ACCOUNT_LIST/designation_accounts"

interface DonationsApi {
    // region Donations

    @GET(PATH_DONATIONS)
    suspend fun getDonationsAsync(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<Donation>>

    @POST(PATH_DONATIONS)
    suspend fun addDonationAsync(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @Body donation: Donation
    ): Response<JsonApiObject<Donation>>

    @PUT(PATH_DONATION)
    suspend fun updateDonationAsync(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @Path(PARAM_DONATION_ID) donationId: String,
        @Body donation: JsonApiObject<Donation>
    ): Response<JsonApiObject<Donation>>

    // endregion Donations

    // region DesignationAccounts
    @GET(PATH_DESIGNATION_ACCOUNTS)
    suspend fun getDesignationAccounts(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<DesignationAccount>>
    // endregion DesignationAccounts
}
