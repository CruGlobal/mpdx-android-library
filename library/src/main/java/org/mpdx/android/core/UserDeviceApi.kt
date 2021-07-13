package org.mpdx.android.core

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.core.model.UserDevice
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserDeviceApi {
    @POST("$PATH_USER/devices")
    suspend fun registerUserDevice(@Body device: UserDevice): Response<JsonApiObject<UserDevice>>
}
