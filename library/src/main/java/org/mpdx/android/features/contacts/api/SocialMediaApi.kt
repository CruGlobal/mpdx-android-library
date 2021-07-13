package org.mpdx.android.features.contacts.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.features.contacts.model.FacebookAccount
import org.mpdx.android.features.contacts.model.LinkedInAccount
import org.mpdx.android.features.contacts.model.TwitterAccount
import org.mpdx.android.features.contacts.model.Website
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

private const val PATH_FACEBOOK_ACCOUNTS = "$PATH_PERSON/facebook_accounts"
private const val PARAM_FACEBOOK_ACCOUNT_ID = "facebook_id"
private const val PATH_FACEBOOK_ACCOUNT = "$PATH_FACEBOOK_ACCOUNTS/{$PARAM_FACEBOOK_ACCOUNT_ID}"

private const val PATH_LINKEDIN_ACCOUNTS = "$PATH_PERSON/linkedin_accounts"
private const val PARAM_LINKEDIN_ACCOUNT_ID = "linkedin_id"
private const val PATH_LINKEDIN_ACCOUNT = "$PATH_LINKEDIN_ACCOUNTS/{$PARAM_LINKEDIN_ACCOUNT_ID}"

private const val PATH_TWITTER_ACCOUNTS = "$PATH_PERSON/twitter_accounts"
private const val PARAM_TWITTER_ACCOUNT_ID = "twitter_id"
private const val PATH_TWITTER_ACCOUNT = "$PATH_TWITTER_ACCOUNTS/{$PARAM_TWITTER_ACCOUNT_ID}"

private const val PATH_WEBSITES = "$PATH_PERSON/websites"
private const val PARAM_WEBSITE_ID = "website_id"
private const val PATH_WEBSITE = "$PATH_WEBSITES/{$PARAM_WEBSITE_ID}"

interface SocialMediaApi {
    // region Facebook APIs

    @POST(PATH_FACEBOOK_ACCOUNTS)
    suspend fun createFacebookAccount(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Body facebook: FacebookAccount
    ): Response<JsonApiObject<FacebookAccount>>

    @PUT(PATH_FACEBOOK_ACCOUNT)
    suspend fun updateFacebookAccount(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_FACEBOOK_ACCOUNT_ID) facebookId: String,
        @Body facebookAccount: JsonApiObject<FacebookAccount>
    ): Response<JsonApiObject<FacebookAccount>>

    @DELETE(PATH_FACEBOOK_ACCOUNT)
    suspend fun deleteFacebookAccount(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_FACEBOOK_ACCOUNT_ID) facebookId: String
    ): Response<JsonApiObject<FacebookAccount>>

    // endregion Facebook APIs

    // region LinkedIn APIs

    @POST(PATH_LINKEDIN_ACCOUNTS)
    suspend fun createLinkedInAccount(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Body linkedIn: LinkedInAccount
    ): Response<JsonApiObject<LinkedInAccount>>

    @PUT(PATH_LINKEDIN_ACCOUNT)
    suspend fun updateLinkedInAccount(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_LINKEDIN_ACCOUNT_ID) linkedinId: String,
        @Body linkedInAccount: JsonApiObject<LinkedInAccount>
    ): Response<JsonApiObject<LinkedInAccount>>

    @DELETE(PATH_LINKEDIN_ACCOUNT)
    suspend fun deleteLinkedInAccount(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_LINKEDIN_ACCOUNT_ID) linkedInId: String
    ): Response<JsonApiObject<LinkedInAccount>>

    // endregion LinkedIn APIs

    // region Twitter APIs

    @POST(PATH_TWITTER_ACCOUNTS)
    suspend fun createTwitterAccount(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Body twitter: TwitterAccount
    ): Response<JsonApiObject<TwitterAccount>>

    @PUT(PATH_TWITTER_ACCOUNT)
    suspend fun updateTwitterAccount(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_TWITTER_ACCOUNT_ID) twitterId: String,
        @Body twitterAccount: JsonApiObject<TwitterAccount>
    ): Response<JsonApiObject<TwitterAccount>>

    @DELETE(PATH_TWITTER_ACCOUNT)
    suspend fun deleteTwitterAccount(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_TWITTER_ACCOUNT_ID) twitterId: String
    ): Response<JsonApiObject<TwitterAccount>>

    // endregion Twitter APIs

    // region Website APIs

    @POST(PATH_WEBSITES)
    suspend fun createWebsite(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Body website: Website
    ): Response<JsonApiObject<Website>>

    @PUT(PATH_WEBSITE)
    suspend fun updateWebsite(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_WEBSITE_ID) websiteId: String,
        @Body website: JsonApiObject<Website>
    ): Response<JsonApiObject<Website>>

    @DELETE(PATH_WEBSITE)
    suspend fun deleteWebsite(
        @Path(PARAM_CONTACT_ID) contactId: String,
        @Path(PARAM_PERSON_ID) personId: String,
        @Path(PARAM_WEBSITE_ID) websiteId: String
    ): Response<JsonApiObject<Website>>

    // endregion Website APIs
}
