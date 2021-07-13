package org.mpdx.android.features.contacts.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.features.contacts.model.EmailAddress
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

private const val PATH_EMAIL_ADDRESSES = "$PATH_PERSON/email_addresses"
private const val PARAM_EMAIL_ADDRESS_ID = "email_id"
private const val PATH_EMAIL_ADDRESS = "$PATH_EMAIL_ADDRESSES/{$PARAM_EMAIL_ADDRESS_ID}"

interface EmailAddressesApi {
    @POST(PATH_EMAIL_ADDRESSES)
    suspend fun createEmailAddress(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Body email: EmailAddress
    ): Response<JsonApiObject<EmailAddress>>

    @PUT(PATH_EMAIL_ADDRESS)
    suspend fun updateEmailAddress(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_EMAIL_ADDRESS_ID) emailAddressId: String,
        @Body address: JsonApiObject<EmailAddress>
    ): Response<JsonApiObject<EmailAddress>>

    @DELETE(PATH_EMAIL_ADDRESS)
    suspend fun deleteEmailAddress(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_EMAIL_ADDRESS_ID) emailAddressId: String
    ): Response<JsonApiObject<EmailAddress>>
}
