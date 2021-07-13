package org.mpdx.android.features.contacts.viewmodel

import androidx.databinding.Bindable
import java.util.Locale
import org.mpdx.android.BR
import org.mpdx.android.R
import org.mpdx.android.base.databinding.ValidationHelper
import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.features.contacts.model.Person

class PersonViewModel : ChangeAwareViewModel<Person>() {
    override fun createModel() = Person()
    override fun updateRelated(model: Person?) {
        emailAddresses.managed = model?.getEmailAddresses()?.asLiveData()
        phoneNumbers.managed = model?.getPhoneNumbers()?.asLiveData()
        facebookAccounts.managed = model?.getFacebookAccounts()?.asLiveData()
        linkedInAccounts.managed = model?.getLinkedInAccounts()?.asLiveData()
        twitterAccounts.managed = model?.getTwitterAccounts()?.asLiveData()
        websites.managed = model?.getWebsites()?.asLiveData()
    }

    override val hasChanges: Boolean
        get() = super.hasChanges ||
            (emailAddresses.isInitialized && emailAddresses.hasChanges) ||
            phoneNumbers.hasChanges

    // region Model Properties
    @get:Bindable
    var firstName by modelStringProperty(BR.firstName) { it::firstName }
    @get:Bindable
    var lastName by modelStringProperty(BR.lastName) { it::lastName }
    @get:Bindable
    val avatar by modelStringProperty(BR.avatar) { it::avatarUrl }
    @get:Bindable
    val birthday by modelNullableProperty(BR.birthday) { it::birthday }

    val anniversary by modelNullableProperty { it::anniversary }
    // endregion Model Properties

    // region Generated Properties
    @get:Bindable("firstName", "lastName")
    val fullName get() = arrayOf(firstName, lastName).filterNotNull().joinToString(" ")
    // endregion Generated Properties

    // region EmailAddresses
    @get:Bindable
    val emailAddresses = LazyRelatedModels(BR.emailAddresses) {
        EmailAddressViewModel(this).also {
            it.allowNullModel = allowNullModel
            it.forceUnmanaged = forceUnmanaged
            it.trackingChanges = trackingChanges
        }
    }

    fun setPrimaryEmailAddress(id: String) = emailAddresses.viewModels.forEach { it.primary = it.model?.id == id }

    internal val duplicateEmailAddresses
        get() = emailAddresses.models.groupingBy { it.email.orEmpty().toLowerCase(Locale.ROOT) }
            .eachCount()
            .filter { it.key.isNotBlank() && it.value > 1 }
            .keys
    // endregion EmailAddresses

    // region PhoneNumbers
    @get:Bindable
    val phoneNumbers = RelatedModels(BR.phoneNumbers) {
        PhoneNumberViewModel().also {
            it.allowNullModel = allowNullModel
            it.forceUnmanaged = forceUnmanaged
            it.trackingChanges = trackingChanges
        }
    }

    fun setPrimaryPhoneNumber(id: String) = phoneNumbers.viewModels.forEach { it.primary = it.model?.id == id }
    // endregion PhoneNumbers

    // region FacebookAccounts
    @get:Bindable
    val facebookAccounts = RelatedModels(BR.facebookAccounts) {
        FacebookAccountViewModel().also {
            it.allowNullModel = allowNullModel
            it.forceUnmanaged = forceUnmanaged
            it.trackingChanges = trackingChanges
        }
    }
    // endregion FacebookAccounts

    // region LinkedInAccounts
    @get:Bindable
    val linkedInAccounts = RelatedModels(BR.linkedInAccounts) {
        LinkedInAccountViewModel().also {
            it.allowNullModel = allowNullModel
            it.forceUnmanaged = forceUnmanaged
            it.trackingChanges = trackingChanges
        }
    }
    // endregion LinkedInAccounts

    // region TwitterAccounts
    @get:Bindable
    val twitterAccounts = RelatedModels(BR.twitterAccounts) {
        TwitterAccountViewModel().also {
            it.allowNullModel = allowNullModel
            it.forceUnmanaged = forceUnmanaged
            it.trackingChanges = trackingChanges
        }
    }
    // endregion TwitterAccounts

    // region Websites
    @get:Bindable
    val websites = RelatedModels(BR.websites) {
        WebsiteViewModel().also {
            it.allowNullModel = allowNullModel
            it.forceUnmanaged = forceUnmanaged
            it.trackingChanges = trackingChanges
        }
    }
    // endregion Websites

    // region Validation
    override fun hasErrors() = firstNameValidation.hasError ||
        emailAddresses.viewModels.any { it.hasErrors() } || phoneNumbers.viewModels.any { it.hasErrors() }
    override fun showErrors() {
        firstNameValidation.showErrors = true
        emailAddresses.viewModels.forEach { it.showErrors() }
        phoneNumbers.viewModels.forEach { it.showErrors() }
    }

    @get:Bindable("firstName")
    val firstNameValidation = ValidationHelper {
        when {
            model?.firstName.isNullOrEmpty() -> R.string.contact_editor_error_person_first_name_blank
            else -> null
        }
    }
    // endregion Validation
}
