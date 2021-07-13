package org.mpdx.android.features.contacts

import org.mpdx.android.features.contacts.model.EmailAddress

interface EmailAddressClickListener {
    fun onEmailAddressClicked(emailAddress: EmailAddress?)
}
