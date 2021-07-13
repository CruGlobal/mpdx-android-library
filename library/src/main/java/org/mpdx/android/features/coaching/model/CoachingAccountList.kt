package org.mpdx.android.features.coaching.model

import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.core.model.AccountList

private const val JSON_API_TYPE_ACCOUNT_LIST_COACHES = "account_list_coaches"

@JsonApiType(JSON_API_TYPE_ACCOUNT_LIST_COACHES)
class CoachingAccountList {
    companion object {
        const val JSON_ACCOUNT_LIST = "account_list"
    }

    @JsonApiAttribute(JSON_ACCOUNT_LIST)
    var accountList: AccountList? = null
}
