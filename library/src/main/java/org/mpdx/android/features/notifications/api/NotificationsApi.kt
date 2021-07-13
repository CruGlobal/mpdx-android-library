package org.mpdx.android.features.notifications.api

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.core.PATH_USER
import org.mpdx.android.features.notifications.model.UserNotification
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.QueryMap

private const val PATH_NOTIFICATIONS = "$PATH_USER/notifications"
private const val PARAM_NOTIFICATION_ID = "notificationId"
private const val PATH_NOTIFICATION = "$PATH_NOTIFICATIONS/{$PARAM_NOTIFICATION_ID}"

interface NotificationsApi {
    @GET(PATH_NOTIFICATIONS)
    suspend fun getNotifications(@QueryMap params: Map<String, String>): Response<JsonApiObject<UserNotification>>

    @PUT(PATH_NOTIFICATION)
    suspend fun updateNotification(
        @Path(PARAM_NOTIFICATION_ID) id: String,
        @QueryMap params: Map<String, String>,
        @Body notification: JsonApiObject<UserNotification>
    ): Response<JsonApiObject<UserNotification>>
}
