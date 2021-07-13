package org.mpdx.android.features.appeals.details

import org.mpdx.android.features.appeals.model.AskedContact

interface AddCommitmentListener {
    fun onAddCommitment(contact: AskedContact?)
}
