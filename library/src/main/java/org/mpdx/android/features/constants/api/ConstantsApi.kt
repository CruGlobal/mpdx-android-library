package org.mpdx.android.features.constants.api

import java.util.Locale
import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.features.constants.model.ConstantList
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

private const val PATH_CONSTANTS = "constants"

interface ConstantsApi {
    @GET(PATH_CONSTANTS)
    suspend fun getConstants(@Header("Accept-Language") locale: Locale): Response<JsonApiObject<ConstantList>>
}
