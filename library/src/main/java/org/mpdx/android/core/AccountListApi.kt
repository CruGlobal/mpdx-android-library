package org.mpdx.android.core

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.PARAM_ACCOUNT_LIST_ID
import org.mpdx.android.base.api.ApiConstants.PATH_ACCOUNT_LIST
import org.mpdx.android.base.api.ApiConstants.PATH_ACCOUNT_LISTS
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.core.model.User
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface AccountListApi {
    @GET(PATH_ACCOUNT_LISTS)
    suspend fun getAccountLists(@QueryMap params: Map<String, String>): Response<JsonApiObject<AccountList>>

    @GET("$PATH_ACCOUNT_LIST/users")
    suspend fun getUsers(
        @Path(PARAM_ACCOUNT_LIST_ID) accountListId: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<User>>
}
