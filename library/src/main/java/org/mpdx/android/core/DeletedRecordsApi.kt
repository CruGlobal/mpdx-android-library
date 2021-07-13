package org.mpdx.android.core

import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.core.model.DeletedRecord
import org.threeten.bp.Instant
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

private const val PATH_DELETED_RECORDS = "deleted_records"

private const val JSON_FILTER_TYPE = "filter[types]"
private const val JSON_FILTER_SINCE = "filter[since_date]"

interface DeletedRecordsApi {
    @GET(PATH_DELETED_RECORDS)
    suspend fun getDeletedRecords(
        @Query(JSON_FILTER_TYPE) type: String,
        @Query(JSON_FILTER_SINCE) since: Instant,
        @QueryMap params: Map<String, String>
    ): Response<JsonApiObject<DeletedRecord>>
}
