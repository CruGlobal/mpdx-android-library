package org.mpdx.android.features.appeals.viewmodel

import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.features.appeals.model.Pledge
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel
import org.mpdx.android.utils.toLocalDate

class PledgeViewModel : ChangeAwareViewModel<Pledge>() {
    override fun updateRelated(model: Pledge?) {
        lazyContact.model = model?.contact
    }

    // region Model Properties
    val amount get() = model?.amount
    // endregion Model Properties

    // region Transformed Property
    val expectedDate get() = model?.expectedDate?.toLocalDate()
    // endregion Transformed Property

    // region Related Models
    private val lazyContact = LazyViewModel { ContactViewModel() }
    val contact get() = lazyContact.viewModel
    // endregion Related Models
}
