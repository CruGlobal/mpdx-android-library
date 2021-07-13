package org.mpdx.android.core

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.FILTER_ACCOUNT_LIST_ID
import org.mpdx.android.core.model.Tag
import org.mpdx.android.features.contacts.api.PATH_CONTACTS
import org.mpdx.android.features.tasks.api.PATH_TASKS
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TagsApi {
    @GET("$PATH_CONTACTS/tags")
    suspend fun getContactTagsAsync(
        @Query(FILTER_ACCOUNT_LIST_ID) accountListId: String
    ): Response<JsonApiObject<Tag>>

    @GET("$PATH_TASKS/tags")
    suspend fun getTaskTagsAsync(
        @Query(FILTER_ACCOUNT_LIST_ID) accountListId: String
    ): Response<JsonApiObject<Tag>>
}
