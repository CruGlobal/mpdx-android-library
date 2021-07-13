package org.mpdx.android.features.donations

import org.mpdx.android.features.donations.model.Donation

interface DonationClickedListener {
    fun onDonationClicked(donation: Donation?)
}
