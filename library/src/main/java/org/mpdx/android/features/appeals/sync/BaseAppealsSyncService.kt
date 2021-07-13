package org.mpdx.android.features.appeals.sync

import android.os.Bundle
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.appeals.api.AppealsApi

private const val ARG_APPEAL_ID = "appeal_id"

abstract class BaseAppealsSyncService(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    protected val appealsApi: AppealsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    protected fun Bundle.putAppealId(appealId: String?) = apply { putString(ARG_APPEAL_ID, appealId) }
    protected fun Bundle.getAppealId(): String? = getString(ARG_APPEAL_ID)
}
