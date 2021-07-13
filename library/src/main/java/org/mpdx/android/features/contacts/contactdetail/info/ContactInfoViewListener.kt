package org.mpdx.android.features.contacts.contactdetail.info

import androidx.annotation.StringRes
import org.mpdx.android.features.contacts.AddressClickListener
import org.mpdx.android.features.contacts.EmailAddressClickListener
import org.mpdx.android.features.contacts.model.Address
import org.mpdx.android.features.contacts.model.PhoneNumber

interface ContactInfoViewListener : AddressClickListener, EmailAddressClickListener {
    fun openPhoneCallApp(phoneNumbers: List<PhoneNumber>?)
    fun openTextMessageApp(phoneNumbers: List<PhoneNumber>?)
    fun openEmailApp(email: String?)
    fun openDirectionsTo(address: Address?)
    fun showMessageDialog(@StringRes message: Int)
    fun openFacebookMessenger(url: String?)
}
