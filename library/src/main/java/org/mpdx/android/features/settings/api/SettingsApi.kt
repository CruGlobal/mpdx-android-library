package org.mpdx.android.features.settings.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.PARAM_ACCOUNT_LIST_ID
import org.mpdx.android.base.api.ApiConstants.PATH_ACCOUNT_LIST
import org.mpdx.android.features.settings.model.NotificationPreference
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.QueryMap

private const val PATH_NOTIFICATION_PREFERENCES = "$PATH_ACCOUNT_LIST/notification_preferences"

private const val PARAM_NOTIFICATION_PREFERENCE_ID = "pref_id"
private const val PATH_NOTIFIACTION_PREFERENCE = "$PATH_NOTIFICATION_PREFERENCES/{$PARAM_NOTIFICATION_PREFERENCE_ID}"

interface SettingsApi {
    @GET(PATH_NOTIFICATION_PREFERENCES)
    suspend fun getNotificationPreferencesAsync(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<NotificationPreference>>

    @PUT(PATH_NOTIFIACTION_PREFERENCE)
    suspend fun updateNotificationPreferencesAsync(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @Path(PARAM_NOTIFICATION_PREFERENCE_ID) prefId: String,
        @QueryMap params: Map<String, String>,
        @Body preference: JsonApiObject<NotificationPreference>
    ): Response<JsonApiObject<NotificationPreference>>
}
