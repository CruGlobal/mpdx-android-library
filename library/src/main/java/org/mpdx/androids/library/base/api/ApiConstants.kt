package org.mpdx.androids.library.base.api

internal object ApiConstants {
    const val PARAM_ACCOUNT_LIST_ID = "account_list_id"
    const val FILTER_ACCOUNT_LIST_ID = "filter[account_list_id]"

    const val PATH_ACCOUNT_LISTS = "account_lists"
    const val PATH_ACCOUNT_LIST = "$PATH_ACCOUNT_LISTS/{$PARAM_ACCOUNT_LIST_ID}"

    // common attributes
    const val JSON_ATTR_CREATED_AT = "created_at"
    const val JSON_ATTR_UPDATED_AT = "updated_at"
    const val JSON_ATTR_UPDATED_IN_DB_AT = "updated_in_db_at"

    // common errors
    const val ERROR_DUPLICATE_UNIQUE_KEY = "duplicate key value violates unique constraint"
    const val ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX =
        "Updated in db at is not equal to the current value in the database"
    const val ERROR_NO_TIME_INFORMATION = "no time information in \"\""
}
