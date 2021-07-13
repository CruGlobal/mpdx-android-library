package org.mpdx.android.features.appeals

import org.mpdx.android.features.appeals.model.Appeal

interface AppealSelectedListener {
    fun onAppealSelected(appeal: Appeal?)
}
