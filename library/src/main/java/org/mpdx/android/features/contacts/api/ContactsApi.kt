package org.mpdx.android.features.contacts.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.FILTER_ACCOUNT_LIST_ID
import org.mpdx.android.features.contacts.model.Address
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.Person
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

const val PATH_CONTACTS = "contacts"
const val PARAM_CONTACT_ID = "contact_id"
private const val PATH_CONTACT = "$PATH_CONTACTS/{$PARAM_CONTACT_ID}"

private const val PATH_ADDRESSES = "$PATH_CONTACT/addresses"
private const val PARAM_ADDRESS_ID = "address_id"
private const val PATH_ADDRESS = "$PATH_ADDRESSES/{$PARAM_ADDRESS_ID}"

private const val PATH_PEOPLE = "$PATH_CONTACT/people"
const val PARAM_PERSON_ID = "person_id"
const val PATH_PERSON = "$PATH_PEOPLE/{$PARAM_PERSON_ID}"

interface ContactsApi {
    // region Contact APIs
    @GET(PATH_CONTACTS)
    suspend fun getContacts(
        @Query(FILTER_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<Contact>>

    @GET(PATH_CONTACT)
    suspend fun getContact(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<Contact>>

    @POST(PATH_CONTACTS)
    suspend fun createContact(
        @QueryMap params: Map<String, String>,
        @Body contact: Contact
    ): Response<JsonApiObject<Contact>>

    @PUT(PATH_CONTACT)
    suspend fun updateContact(
        @Path(PARAM_CONTACT_ID) id: String,
        @QueryMap params: Map<String, String>,
        @Body contact: JsonApiObject<Contact>
    ): Response<JsonApiObject<Contact>>
    // endregion Contact APIs

    // region People APIs
    @POST(PATH_PEOPLE)
    suspend fun createPersonAsync(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Body person: Person
    ): Response<JsonApiObject<Person>>

    @PUT(PATH_PERSON)
    suspend fun updatePersonAsync(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Body person: JsonApiObject<Person>
    ): Response<JsonApiObject<Person>>

    @DELETE(PATH_PERSON)
    suspend fun deletePersonAsync(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String
    ): Response<JsonApiObject<Person>>
    // endregion People APIs

    // region Address APIs

    @POST(PATH_ADDRESSES)
    suspend fun createAddressAsync(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Body address: Address
    ): Response<JsonApiObject<Address>>

    @PUT(PATH_ADDRESS)
    suspend fun updateAddressAsync(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_ADDRESS_ID) addressId: String,
        @Body address: JsonApiObject<Address>
    ): Response<JsonApiObject<Address>>

    @DELETE(PATH_ADDRESS)
    suspend fun deleteAddressAsync(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_ADDRESS_ID) addressId: String
    ): Response<JsonApiObject<Address>>

    // endregion Address APIs
}
