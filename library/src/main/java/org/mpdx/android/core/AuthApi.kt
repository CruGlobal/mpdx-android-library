package org.mpdx.android.core

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.core.model.Authenticate
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

const val PATH_AUTHENTICATE = "user/authenticate"

interface AuthApi {
    @POST(PATH_AUTHENTICATE)
    fun authenticate(@Body body: Authenticate): Call<JsonApiObject<Authenticate>>
}
