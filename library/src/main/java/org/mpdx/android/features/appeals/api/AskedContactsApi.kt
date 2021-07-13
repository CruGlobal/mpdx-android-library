package org.mpdx.android.features.appeals.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.features.appeals.model.AskedContact
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

private const val PATH_ASKED_CONTACTS = "$PATH_APPEAL/appeal_contacts"

interface AskedContactsApi {
    @GET(PATH_ASKED_CONTACTS)
    suspend fun getAskedContacts(
        @Path(PARAM_APPEAL_ID) appealId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<AskedContact>>

    @POST(PATH_ASKED_CONTACTS)
    suspend fun addAskedContact(
        @Path(PARAM_APPEAL_ID) appealsId: String,
        @Body contact: AskedContact,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<AskedContact>>
}
