package org.mpdx.android.features.contacts.viewmodel

import android.telephony.PhoneNumberUtils.formatNumber
import androidx.databinding.Bindable
import java.util.Locale
import org.mpdx.android.BR
import org.mpdx.android.R
import org.mpdx.android.base.databinding.ValidationHelper
import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.features.contacts.model.PhoneNumber

class PhoneNumberViewModel : ChangeAwareViewModel<PhoneNumber>() {
    override fun createModel() = PhoneNumber()
    override fun updateRelated(model: PhoneNumber?) {
        lazyPerson.model = model?.person
    }

    // region Model Properties
    @get:Bindable
    var number by modelStringProperty(BR.number) { it::number }
    val location get() = model?.location
    @get:Bindable
    var primary by modelNullableBooleanProperty(BR.primary) { it::isPrimary }
    val isHistoric get() = model?.isHistoric
    @get:Bindable
    val isEditable
        get() = model?.isEditable ?: false
    // endregion Model Properties

    // region Transformed Properties
    @get:Bindable("number")
    val formattedNumber get() = formatNumber(number, Locale.getDefault().country)
    // endregion Transformed Properties

    // region Related Models
    private val lazyPerson = LazyViewModel { PersonViewModel() }
    val person get() = lazyPerson.viewModel
    // endregion Related Models

    // region Validation
    override fun hasErrors() = numberValidation.hasError
    override fun showErrors() {
        numberValidation.showErrors = true
    }

    @get:Bindable("number")
    val numberValidation = ValidationHelper {
        when {
            model?.number.isNullOrBlank() -> R.string.contact_editor_error_phone_blank
            else -> null
        }
    }
    // endregion Validation
}
