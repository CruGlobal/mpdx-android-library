package org.mpdx.android.features.contacts.viewmodel

import androidx.databinding.Bindable
import java.util.Locale
import org.mpdx.android.BR
import org.mpdx.android.R
import org.mpdx.android.base.databinding.ValidationHelper
import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.features.contacts.model.EmailAddress

class EmailAddressViewModel constructor(private val parentPersonViewModel: PersonViewModel? = null) :
    ChangeAwareViewModel<EmailAddress>() {
    override fun createModel() = EmailAddress()
    override fun updateRelated(model: EmailAddress?) {
        lazyPerson.model = model?.person
    }

    // region Model Properties
    @get:Bindable
    var email by modelStringProperty(BR.email) { it::email }
    val location get() = model?.location
    @get:Bindable
    var primary by modelNullableBooleanProperty(BR.primary) { it::isPrimary }
    val isHistoric get() = model?.isHistoric
    @get:Bindable
    val isEditable get() = model?.isEditable ?: false
    // endregion Model Properties

    // region Related Models
    private val lazyPerson = LazyViewModel { PersonViewModel() }
    val person get() = parentPersonViewModel ?: lazyPerson.viewModel
    // endregion Related Models

    // region Validation
    override fun hasErrors() = emailValidation.hasError
    override fun showErrors() {
        emailValidation.showErrors = true
    }

    @get:Bindable("email")
    val emailValidation = ValidationHelper {
        when {
            model?.email.isNullOrEmpty() -> R.string.contact_editor_error_email_blank
            model?.email?.contains("[^\\s]@[^\\s]".toRegex()) == false -> R.string.contact_editor_error_email_invalid
            model?.email?.toLowerCase(Locale.ROOT) in person.duplicateEmailAddresses ->
                R.string.contact_editor_error_email_exists
            else -> null
        }
    }
    // endregion Validation
}
