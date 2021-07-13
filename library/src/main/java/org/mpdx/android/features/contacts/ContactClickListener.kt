package org.mpdx.android.features.contacts

import org.mpdx.android.features.contacts.model.Contact

interface ContactClickListener {
    fun onContactClick(contact: Contact?)
}
