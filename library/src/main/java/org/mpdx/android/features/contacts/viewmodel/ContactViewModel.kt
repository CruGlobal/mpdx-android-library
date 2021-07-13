package org.mpdx.android.features.contacts.viewmodel

import androidx.databinding.Bindable
import androidx.lifecycle.map
import org.ccci.gto.android.common.androidx.lifecycle.databinding.getPropertyLiveData
import org.mpdx.android.BR
import org.mpdx.android.R
import org.mpdx.android.base.databinding.ValidationHelper
import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.base.model.addTag
import org.mpdx.android.base.model.removeTag
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.features.constants.Constants
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.realm.sortByCreated

class ContactViewModel : ChangeAwareViewModel<Contact>() {
    override fun createModel() = Contact()
    override fun updateRelated(model: Contact?) {
        addresses.managed = model?.getAddresses()?.sortByCreated()?.asLiveData()
        people.managed = model?.getPeople()?.asLiveData()
    }

    override val hasChanges
        get() = super.hasChanges ||
            (people.isInitialized && people.hasChanges) ||
            (addresses.isInitialized && addresses.hasChanges)

    // region Model Properties
    @get:Bindable
    var name by modelStringProperty(BR.name) { it::name }
    val avatar by modelStringProperty { it::squareAvatar }
    val isStarred by modelBooleanProperty { it::isStarred }
    @get:Bindable
    var greeting by modelStringProperty(BR.greeting) { it::greeting }
    @get:Bindable
    var envelopeGreeting by modelStringProperty(BR.envelopeGreeting) { it::envelopeGreeting }
    @get:Bindable
    var commitmentAmount by modelStringProperty(BR.commitmentAmount) { it::pledgeAmount }
    @get:Bindable
    var commitmentReceived by modelNullableBooleanProperty(BR.commitmentReceived) { it::pledgeReceived }
    val donationLateAt get() = model?.donationLateAt
    val donationLateState get() = model?.donationLateState
    @get:Bindable
    var noAppeals by modelNullableBooleanProperty(BR.noAppeals) { it::noAppeals }
    @get:Bindable
    var notes by modelStringProperty(BR.notes) { it::notes }

    private var newsletterValue by modelStringProperty(BR.newsletter) { it::sendNewsletter }
    private var statusValue by modelStringProperty(BR.status) { it::status }
    private var localeCode by modelStringProperty(BR.locale) { it::locale }
    var commitmentCurrencyCode by modelStringProperty(BR.commitmentCurrency) { it::pledgeCurrency }
        private set
    private var commitmentFrequencyValue by modelStringProperty(BR.commitmentFrequency) { it::pledgeFrequency }
    private var likelyToGiveValue by modelStringProperty(BR.likelyToGive) { it::likelyToGive }
    // endregion Model Properties

    // region Transformed Properties
    @get:Bindable
    var newsletter: String?
        get() = Constants.getNewsletterOptionLabel(newsletterValue)
        set(value) {
            newsletterValue = Constants.getNewsletterOptionValue(value)
        }

    @get:Bindable
    var status: String?
        get() = Constants.getStatusLabel(statusValue)
        set(value) {
            statusValue = Constants.getStatusValue(value)
        }

    @get:Bindable
    var commitmentCurrency
        get() = Constants.getCurrencyLabel(commitmentCurrencyCode)
        set(value) {
            commitmentCurrencyCode = Constants.getCurrencyCode(value)
        }

    @get:Bindable
    var commitmentFrequency: String?
        get() = Constants.getCommitmentFrequencyLabel(commitmentFrequencyValue)
        set(value) {
            commitmentFrequencyValue = Constants.getCommitmentFrequencyValue(value)
        }

    @get:Bindable
    var likelyToGive: String?
        get() = Constants.getLikelinessToGiveLabel(likelyToGiveValue)
        set(value) {
            likelyToGiveValue = Constants.getLikelinessToGiveValue(value)
        }

    @get:Bindable
    var locale: String?
        get() = Constants.getLocaleName(localeCode)
        set(value) {
            localeCode = Constants.getLocaleCode(value)
        }
    // endregion Transformed Properties

    // region Generated Properties
    @get:Bindable("commitmentAmount", "commitmentCurrency", "commitmentFrequency")
    val commitment: String?
        get() = arrayOf(commitmentAmount, commitmentCurrencyCode, commitmentFrequency)
            .filterNot { it.isNullOrEmpty() }.joinToString(" ")

    @get:Bindable("status", "commitment")
    val relationshipSynopsis: String?
        get() = arrayOf(status, commitment).filterNot { it.isNullOrEmpty() }.joinToString(" | ")
    // endregion Generated Properties

    // region Addresses
    @get:Bindable
    val addresses = LazyRelatedModels(BR.addresses) {
        AddressViewModel().also {
            it.allowNullModel = allowNullModel
            it.forceUnmanaged = forceUnmanaged
            it.trackingChanges = trackingChanges
        }
    }

    val addressViewModelsLiveData by lazy {
        addresses.initialize()
        getPropertyLiveData(::addresses, BR.addresses).map { it.viewModels }
    }

    fun setPrimaryAddress(id: String) = addresses.viewModels.forEach { it.primary = it.model?.id == id }
    // endregion Addresses

    // region People
    @get:Bindable
    val people = LazyRelatedModels(BR.people) {
        PersonViewModel().also {
            it.allowNullModel = allowNullModel
            it.forceUnmanaged = forceUnmanaged
            it.trackingChanges = trackingChanges
        }
    }

    val peopleViewModelsLiveData by lazy {
        people.initialize()
        getPropertyLiveData(::people, BR.people).map { it.viewModels }
    }
    // endregion People

    // region Tags
    @get:Bindable
    val tags: List<String> get() = model?.tags.orEmpty()
    val tagsLiveData by lazy { getPropertyLiveData(::tags, BR.tags) }

    fun addTag(tag: String) {
        if (model?.addTag(tag) == true) notifyPropertyChanged(BR.tags)
    }

    fun deleteTag(tag: String) {
        if (model?.removeTag(tag) == true) notifyPropertyChanged(BR.tags)
    }
    // endregion Tags

    // region Validation
    override fun hasErrors() =
        nameValidation.hasError ||
            (addresses.isInitialized && addresses.viewModels.any { it.hasErrors() }) ||
            (people.isInitialized && people.viewModels.any { it.hasErrors() })
    override fun showErrors() {
        nameValidation.showErrors = true
        if (addresses.isInitialized) addresses.viewModels.forEach { it.showErrors() }
        if (people.isInitialized) people.viewModels.forEach { it.showErrors() }
    }

    @get:Bindable("name")
    val nameValidation = ValidationHelper {
        when {
            model?.name.isNullOrBlank() -> R.string.contact_editor_error_contact_name_blank
            else -> null
        }
    }
    // endregion Validation
}
