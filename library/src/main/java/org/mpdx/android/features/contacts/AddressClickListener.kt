package org.mpdx.android.features.contacts

import org.mpdx.android.features.contacts.model.Address

interface AddressClickListener {
    fun onAddressClicked(address: Address?)
}
