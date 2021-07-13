package org.mpdx.android.features.contacts.viewmodel

import androidx.databinding.Bindable
import org.mpdx.android.BR
import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.features.contacts.model.LinkedInAccount

class LinkedInAccountViewModel : ChangeAwareViewModel<LinkedInAccount>() {
    override fun createModel() = LinkedInAccount()

    @get:Bindable
    val username by modelStringProperty(BR.username) { it::publicUrl }
}
