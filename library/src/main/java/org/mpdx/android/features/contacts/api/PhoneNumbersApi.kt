package org.mpdx.android.features.contacts.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.features.contacts.model.PhoneNumber
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

private const val PATH_PHONE_NUMBERS = "$PATH_PERSON/phones"
private const val PARAM_PHONE_NUMBER_ID = "phone_id"
private const val PATH_PHONE_NUMBER = "$PATH_PHONE_NUMBERS/{$PARAM_PHONE_NUMBER_ID}"

interface PhoneNumbersApi {
    @POST(PATH_PHONE_NUMBERS)
    suspend fun createPhoneNumber(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Body number: PhoneNumber
    ): Response<JsonApiObject<PhoneNumber>>

    @PUT(PATH_PHONE_NUMBER)
    suspend fun updatePhoneNumber(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_PHONE_NUMBER_ID) phoneNumberId: String,
        @Body address: JsonApiObject<PhoneNumber>
    ): Response<JsonApiObject<PhoneNumber>>

    @DELETE(PATH_PHONE_NUMBER)
    suspend fun deletePhoneNumber(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_PHONE_NUMBER_ID) phoneNumberId: String
    ): Response<JsonApiObject<PhoneNumber>>
}
