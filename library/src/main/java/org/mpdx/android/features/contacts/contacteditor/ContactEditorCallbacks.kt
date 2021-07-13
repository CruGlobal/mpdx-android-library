package org.mpdx.android.features.contacts.contacteditor

import android.widget.TextView
import org.mpdx.android.features.contacts.model.Address
import org.mpdx.android.features.contacts.model.EmailAddress
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.model.PhoneNumber
import org.mpdx.android.features.contacts.viewmodel.AddressViewModel
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel
import org.mpdx.android.features.contacts.viewmodel.EmailAddressViewModel
import org.mpdx.android.features.contacts.viewmodel.PersonViewModel
import org.mpdx.android.features.contacts.viewmodel.PhoneNumberViewModel

interface ContactEditorCallbacks {
    fun importContact()

    fun ContactViewModel?.onCreatePerson()
    fun ContactViewModel?.onUpdatePersonVisibility(person: Person?, visible: Boolean)
    fun ContactViewModel?.onDeletePerson(person: Person?)
    fun ContactViewModel?.onCreateAddress(): AddressViewModel?
    fun ContactViewModel?.onDeleteAddress(address: Address?)
    fun ContactViewModel?.onSetPrimaryAddress(address: Address?)

    fun ContactViewModel?.onAddTag(view: TextView?): Boolean
    fun ContactViewModel?.onRemoveTag(view: TextView?)

    fun PersonViewModel?.onCreateEmailAddress(): EmailAddressViewModel?
    fun PersonViewModel?.onDeleteEmailAddress(email: EmailAddress?)
    fun PersonViewModel?.onSetPrimaryEmailAddress(email: EmailAddress?)
    fun PersonViewModel?.onCreatePhoneNumber(): PhoneNumberViewModel?
    fun PersonViewModel?.onDeletePhoneNumber(number: PhoneNumber?)
    fun PersonViewModel?.onSetPrimaryPhoneNumber(number: PhoneNumber?)

    fun showUneditableAddressMessage()
}
