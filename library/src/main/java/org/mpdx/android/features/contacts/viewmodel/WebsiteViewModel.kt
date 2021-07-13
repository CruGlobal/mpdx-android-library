package org.mpdx.android.features.contacts.viewmodel

import androidx.databinding.Bindable
import org.mpdx.android.BR
import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.features.contacts.model.Website

class WebsiteViewModel : ChangeAwareViewModel<Website>() {
    override fun createModel() = Website()

    @get:Bindable
    val url by modelStringProperty(BR.url) { it::url }
}
