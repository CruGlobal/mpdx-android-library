package org.mpdx.android.features.contacts.people

import org.mpdx.android.features.contacts.model.Person

interface PersonSelectedListener {
    fun onPersonSelected(person: Person?)
}
