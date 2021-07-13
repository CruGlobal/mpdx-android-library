package org.mpdx.android.features.contacts.viewmodel

import androidx.databinding.Bindable
import org.mpdx.android.BR
import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.features.constants.Constants
import org.mpdx.android.features.contacts.model.Address

class AddressViewModel : ChangeAwareViewModel<Address>() {
    override fun createModel() = Address()

    @get:Bindable
    var street by modelStringProperty(BR.street) { it::street }
    @get:Bindable
    var city by modelStringProperty(BR.city) { it::city }
    @get:Bindable
    var state by modelStringProperty(BR.state) { it::state }
    @get:Bindable
    var postalCode by modelStringProperty(BR.postalCode) { it::postalCode }
    @get:Bindable
    var country by modelStringProperty(BR.country) { it::country }
    private var locationValue by modelStringProperty(BR.location) { it::location }
    @get:Bindable
    var primary by modelNullableBooleanProperty(BR.primary) { it::isPrimary }

    val isHistoric get() = model?.isHistoric ?: false
    val isEditable get() = model?.isEditable ?: false

    // region Transformed Properties
    @get:Bindable
    var location: String?
        get() = Constants.getLocationLabel(locationValue)
        set(value) {
            locationValue = Constants.getLocationValue(value)
        }
    // endregion Transformed Properties
}
