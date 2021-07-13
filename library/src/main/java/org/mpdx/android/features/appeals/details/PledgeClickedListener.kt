package org.mpdx.android.features.appeals.details

import org.mpdx.android.features.appeals.model.Pledge

interface PledgeClickedListener {
    fun onPledgeClicked(pledge: Pledge?)
}
