package org.mpdx.android.features.contacts.contacteditor

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.h6ah4i.android.widget.advrecyclerview.composedadapter.ComposedAdapter
import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.combineWith
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.viewpager.adapter.ViewHolderPagerAdapter
import org.mpdx.android.R
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.core.realm.getContactTagsFor
import org.mpdx.android.core.sync.TagsSyncService
import org.mpdx.android.databinding.ContactsEditorFragmentBinding
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.contacts.contacteditor.adapters.AddressDetailsAdapter
import org.mpdx.android.features.contacts.contacteditor.adapters.ContactDetailsAdapter
import org.mpdx.android.features.contacts.contacteditor.adapters.PersonAddAdapter
import org.mpdx.android.features.contacts.contacteditor.adapters.PersonDetailsAdapter
import org.mpdx.android.features.contacts.contactimport.importContact
import org.mpdx.android.features.contacts.model.Address
import org.mpdx.android.features.contacts.model.AddressFields
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.EmailAddress
import org.mpdx.android.features.contacts.model.EmailAddressFields
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.model.PersonFields
import org.mpdx.android.features.contacts.model.PhoneNumber
import org.mpdx.android.features.contacts.model.PhoneNumberFields
import org.mpdx.android.features.contacts.realm.createAddress
import org.mpdx.android.features.contacts.realm.createContact
import org.mpdx.android.features.contacts.realm.createEmailAddress
import org.mpdx.android.features.contacts.realm.createPerson
import org.mpdx.android.features.contacts.realm.createPhoneNumber
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.features.contacts.sync.AddressesSyncService
import org.mpdx.android.features.contacts.sync.ContactsSyncService
import org.mpdx.android.features.contacts.sync.EmailAddressSyncService
import org.mpdx.android.features.contacts.sync.PeopleSyncService
import org.mpdx.android.features.contacts.sync.PhoneNumberSyncService
import org.mpdx.android.features.contacts.viewmodel.AddressViewModel
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel
import org.mpdx.android.features.contacts.viewmodel.EmailAddressViewModel
import org.mpdx.android.features.contacts.viewmodel.PersonViewModel
import org.mpdx.android.features.contacts.viewmodel.PhoneNumberViewModel
import org.mpdx.android.utils.realmTransactionAsync
import splitties.fragmentargs.argOrNull

private const val REQ_IMPORT_CONTACT = 1
private const val REQ_IMPORT_PERSON = 2

// Form page positions
private const val PAGE_DETAILS = 0
private const val PAGE_PEOPLE = 1
private const val PAGE_ADDRESSES = 2

private const val STATE_PEOPLE_EXPANDABLE_STATE = "people_expandable_state"

@AndroidEntryPoint
class ContactEditorFragment() : DataBindingFragment<ContactsEditorFragmentBinding>(), ContactEditorCallbacks {
    constructor(contactId: String? = null) : this() {
        this.contactId = contactId
    }

    var contactId by argOrNull<String>()

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreFormState(savedInstanceState)
        setHasOptionsMenu(true)
        setupUnsavedChangesDialog()
    }

    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ContactsEditorFragmentBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupForm()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.contacts_editor_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_save -> {
            saveContact()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = permissions.filterIndexed { i, _ -> grantResults[i] == PackageManager.PERMISSION_GRANTED }
        when (requestCode) {
            REQ_IMPORT_CONTACT, REQ_IMPORT_PERSON ->
                if (granted.contains(Manifest.permission.READ_CONTACTS)) showContactPicker(requestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQ_IMPORT_CONTACT, REQ_IMPORT_PERSON ->
                if (resultCode == RESULT_OK) importContact(requestCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveFormState(outState)
    }

    override fun onDestroyView() {
        cleanupForm()
        super.onDestroyView()
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel: ContactEditorFragmentDataModel by lazy {
        ViewModelProvider(this).get(ContactEditorFragmentDataModel::class.java)
            .also { it.contactId.value = contactId }
    }
    private val contactViewModel: ContactViewModel by lazy {
        ViewModelProvider(this).get(ContactViewModel::class.java)
            .apply {
                allowNullModel = false
                forceUnmanaged = true
                trackingChanges = true
            }
            .also { dataModel.contact.observe(this, it) }
    }

    // region Save logic
    @Inject
    internal lateinit var appPrefs: AppPrefs
    @Inject
    internal lateinit var contactsSyncService: ContactsSyncService
    @Inject
    internal lateinit var addressesSyncService: AddressesSyncService
    @Inject
    internal lateinit var peopleSyncService: PeopleSyncService
    @Inject
    internal lateinit var emailAddressSyncService: EmailAddressSyncService
    @Inject
    internal lateinit var phoneNumberSyncService: PhoneNumberSyncService

    private fun saveContact() {
        if (contactViewModel.hasErrors()) {
            contactViewModel.showErrors()
            context?.let { Toast.makeText(it, R.string.contact_editor_errors_toast, LENGTH_SHORT).show() }
            return
        }

        val onSuccess: () -> Unit = {
            contactsSyncService.syncDirtyContacts().onComplete {
                addressesSyncService.syncDirtyAddresses().launch()
                peopleSyncService.syncDirtyPeople().onComplete {
                    emailAddressSyncService.syncDirtyEmailAddresses().launch()
                    phoneNumberSyncService.syncDirtyPhoneNumbers().launch()
                }.launch()
            }.launch()
            activity?.finish()
        }
        realmTransactionAsync(onSuccess) {
            val model = contactViewModel.model ?: return@realmTransactionAsync
            val accountList = getAccountList(appPrefs.accountListId).findFirst() ?: return@realmTransactionAsync
            val managedContact = getContact(contactId).findFirst() ?: copyToRealm(createContact(accountList))
            managedContact.mergeChangedFields(model)

            saveAddresses(managedContact, contactViewModel)
            savePeople(managedContact, contactViewModel)
        }
    }

    private fun Realm.saveAddresses(contact: Contact, contactViewModel: ContactViewModel) {
        if (!contactViewModel.addresses.isInitialized) return
        contactViewModel.addresses.deleted.forEach {
            contact.getAddresses()?.equalTo(AddressFields.ID, it)?.findFirst()?.isDeleted = true
        }
        contactViewModel.addresses.viewModels.forEach { saveAddress(contact, it) }
    }

    private fun Realm.saveAddress(contact: Contact, addressViewModel: AddressViewModel) {
        val model = addressViewModel.model ?: return
        val address = contact.getAddresses()?.equalTo(AddressFields.ID, model.id)?.findFirst()
            ?: copyToRealm(createAddress(contact))
        address.mergeChangedFields(model)
    }

    private fun Realm.savePeople(contact: Contact, contactViewModel: ContactViewModel) {
        if (!contactViewModel.people.isInitialized) return
        contactViewModel.people.deleted.forEach {
            contact.getPeople()?.equalTo(PersonFields.ID, it)?.findFirst()?.isDeleted = true
        }
        contactViewModel.people.viewModels.forEach { savePerson(contact, it) }
    }

    private fun Realm.savePerson(contact: Contact, personViewModel: PersonViewModel) {
        val model = personViewModel.model ?: return
        val person = contact.getPeople()?.equalTo(PersonFields.ID, model.id)?.findFirst()
            ?: copyToRealm(createPerson(contact))
        person.mergeChangedFields(model)

        saveEmailAddresses(person, personViewModel)
        savePhoneNumbers(person, personViewModel)
    }

    private fun Realm.saveEmailAddresses(person: Person, personViewModel: PersonViewModel) {
        personViewModel.emailAddresses.deleted.forEach {
            person.getEmailAddresses()?.equalTo(EmailAddressFields.ID, it)?.findFirst()?.isDeleted = true
        }
        personViewModel.emailAddresses.viewModels.forEach { saveEmailAddress(person, it) }
    }

    private fun Realm.saveEmailAddress(person: Person, emailAddressViewModel: EmailAddressViewModel) {
        val model = emailAddressViewModel.model ?: return
        val emailAddress = person.getEmailAddresses()?.equalTo(EmailAddressFields.ID, model.id)?.findFirst()
            ?: copyToRealm(createEmailAddress(person))
        emailAddress.mergeChangedFields(model)
    }

    private fun Realm.savePhoneNumbers(person: Person, personViewModel: PersonViewModel) {
        personViewModel.phoneNumbers.deleted.forEach {
            person.getPhoneNumbers()?.equalTo(PhoneNumberFields.ID, it)?.findFirst()?.isDeleted = true
        }
        personViewModel.phoneNumbers.viewModels.forEach { savePhoneNumber(person, it) }
    }

    private fun Realm.savePhoneNumber(person: Person, phoneNumberViewModel: PhoneNumberViewModel) {
        val model = phoneNumberViewModel.model ?: return
        val phoneNumber = person.getPhoneNumbers()?.equalTo(PhoneNumberFields.ID, model.id)?.findFirst()
            ?: copyToRealm(createPhoneNumber(person))
        phoneNumber.mergeChangedFields(model)
    }
    // endregion Save logic
    // endregion Data Model

    // region Form

    private val formVisibilityViewModel: FormVisibilityViewModel by lazy {
        ViewModelProvider(this).get(FormVisibilityViewModel::class.java)
            .apply { if (contactId != null) importContact.value = false }
    }

    private fun setupForm() {
        binding.form.adapter = FormPagerAdapter()
    }

    private fun cleanupForm() {
        binding.form.adapter = null
        lastPeopleExpandableItemManager?.let {
            lastPeopleExpandableItemManager = null
            it.release()
        }
    }

    private fun saveFormState(outState: Bundle) =
        outState.putParcelable(STATE_PEOPLE_EXPANDABLE_STATE, peopleExpandableItemSavedState)

    private fun restoreFormState(savedInstanceState: Bundle?) {
        peopleExpandableItemSavedState = savedInstanceState?.getParcelable(STATE_PEOPLE_EXPANDABLE_STATE)
    }

    // region Details Section
    private val contactDetailsAdapter by lazy {
        val tags = dataModel.tags.combineWith(contactViewModel.tagsLiveData) { tags, excluded ->
            tags.mapNotNull { it.name }.filterNot { excluded.contains(it) }
        }
        ContactDetailsAdapter(this, this, contactViewModel, tags, formVisibilityViewModel)
    }
    // endregion Details Section

    // region People Section

    private val peopleDetailsAdapter by lazy {
        PersonDetailsAdapter(this, this, contactViewModel, formVisibilityViewModel).apply {
            contactViewModel.peopleViewModelsLiveData.observe(this@ContactEditorFragment, Observer { people = it })
        }
    }

    private val personAddAdapter by lazy { PersonAddAdapter(this, this, contactViewModel) }

    private var lastPeopleExpandableItemManager: RecyclerViewExpandableItemManager? = null
        set(value) {
            field?.let { peopleExpandableItemSavedState = it.savedState }
            field = value
        }
    private var peopleExpandableItemSavedState: Parcelable? = null
        get() = lastPeopleExpandableItemManager?.savedState ?: field

    // endregion People Section

    // region Addresses Section
    private val addressDetailsAdapter by lazy {
        AddressDetailsAdapter(this, contactViewModel).also { adapter ->
            contactViewModel.addressViewModelsLiveData.observe(this) { adapter.addresses = it }
        }
    }
    // endregion Addresses Section

    private inner class FormPagerAdapter : ViewHolderPagerAdapter<SectionViewHolder>() {
        override fun getCount() = 3
        override fun getPageTitle(position: Int) = when (position) {
            PAGE_DETAILS -> R.string.contact_editor_section_details
            PAGE_PEOPLE -> R.string.contact_editor_section_people
            PAGE_ADDRESSES -> R.string.contact_editor_section_addresses
            else -> null
        }?.let { getString(it) }

        override fun onCreateViewHolder(parent: ViewGroup): SectionViewHolder {
            return SectionViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.contacts_editor_section, parent, false) as RecyclerView
            )
        }

        override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            when (position) {
                PAGE_DETAILS -> holder.recyclerView.adapter = contactDetailsAdapter
                PAGE_ADDRESSES -> holder.recyclerView.adapter = addressDetailsAdapter
                PAGE_PEOPLE -> {
                    holder.expandableItemManager = RecyclerViewExpandableItemManager(peopleExpandableItemSavedState)
                    holder.recyclerView.adapter = ComposedAdapter().apply {
                        setHasStableIds(true)
                        addAdapter(holder.expandableItemManager!!.createWrappedAdapter(peopleDetailsAdapter))
                        addAdapter(personAddAdapter)
                    }
                    holder.expandableItemManager?.attachRecyclerView(holder.recyclerView)
                    lastPeopleExpandableItemManager = holder.expandableItemManager
                }
            }
        }

        override fun onViewHolderRecycled(holder: SectionViewHolder) {
            holder.recyclerView.adapter = null
            when (holder.lastKnownPosition) {
                PAGE_PEOPLE -> {
                    if (holder.expandableItemManager == lastPeopleExpandableItemManager) {
                        lastPeopleExpandableItemManager = null
                    }
                    holder.expandableItemManager = null
                }
            }
            super.onViewHolderRecycled(holder)
        }
    }

    private class SectionViewHolder(val recyclerView: RecyclerView) : ViewHolderPagerAdapter.ViewHolder(recyclerView) {
        var expandableItemManager: RecyclerViewExpandableItemManager? = null
            set(value) {
                if (field == value) return
                field?.release()
                field = value
            }

        public override fun getLastKnownPosition() = super.getLastKnownPosition()
    }

    // endregion Form

    // region Contact Import
    private fun showContactPicker(requestCode: Int) {
        val activity = activity ?: return
        val permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), requestCode)
        } else {
            startActivityForResult(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), requestCode)
        }
    }

    private fun importContact(requestCode: Int, data: Intent?) {
        val uri = data?.data ?: return
        val importPersonOnly = requestCode == REQ_IMPORT_PERSON
        val imported = activity?.contentResolver?.importContact(uri, importPersonOnly) ?: return
        contactViewModel.apply {
            // add the imported person
            people.addModel(createPerson())?.apply {
                mergeChangesFrom(imported.person)
                imported.emailAddresses.forEach { emailAddresses.addModel(createEmailAddress())?.mergeChangesFrom(it) }
                imported.phoneNumbers.forEach { phoneNumbers.addModel(createPhoneNumber())?.mergeChangesFrom(it) }
                onUpdatePersonVisibility(model, true)
            }

            // include contact specific data
            if (!importPersonOnly) {
                mergeChangesFrom(imported.contact)
                imported.addresses?.forEach { addresses.addModel(createAddress())?.mergeChangesFrom(it) }
            }
        }

        // hide the import contact button
        if (requestCode == REQ_IMPORT_CONTACT) formVisibilityViewModel.importContact.value = false
    }
    // endregion Contact Import

    // region ContactEditorCallbacks
    override fun importContact() = showContactPicker(REQ_IMPORT_CONTACT)
    override fun ContactViewModel?.onCreatePerson() {
        showAddPersonDialog()
    }

    override fun ContactViewModel?.onCreateAddress() = this?.addresses?.addModel(createAddress())
    override fun PersonViewModel?.onCreateEmailAddress() = this?.emailAddresses?.addModel(createEmailAddress())
    override fun PersonViewModel?.onCreatePhoneNumber() = this?.phoneNumbers?.addModel(createPhoneNumber())

    override fun ContactViewModel?.onSetPrimaryAddress(address: Address?) =
        address?.id?.let { this?.setPrimaryAddress(it) } ?: Unit
    override fun PersonViewModel?.onSetPrimaryEmailAddress(email: EmailAddress?) =
        email?.id?.let { this?.setPrimaryEmailAddress(it) } ?: Unit
    override fun PersonViewModel?.onSetPrimaryPhoneNumber(number: PhoneNumber?) =
        number?.id?.let { this?.setPrimaryPhoneNumber(it) } ?: Unit

    override fun ContactViewModel?.onDeletePerson(person: Person?) = this?.people?.deleteModel(person?.id) ?: Unit
    override fun ContactViewModel?.onDeleteAddress(address: Address?) =
        this?.addresses?.deleteModel(address?.id) ?: Unit
    override fun PersonViewModel?.onDeleteEmailAddress(email: EmailAddress?) =
        this?.emailAddresses?.deleteModel(email?.id) ?: Unit
    override fun PersonViewModel?.onDeletePhoneNumber(number: PhoneNumber?) =
        this?.phoneNumbers?.deleteModel(number?.id) ?: Unit

    override fun ContactViewModel?.onUpdatePersonVisibility(person: Person?, visible: Boolean) {
        if (this == null) return
        val position = person?.id?.let { id -> people.models.indexOfFirst { it.id == id } } ?: return
        if (position < 0) return
        lastPeopleExpandableItemManager?.apply {
            if (visible) expandGroup(position) else collapseGroup(position)
        }
    }

    override fun ContactViewModel?.onAddTag(view: TextView?): Boolean {
        if (this == null) return false
        view?.text?.toString()?.let { addTag(it); view.text = null }
        return true
    }

    override fun ContactViewModel?.onRemoveTag(view: TextView?) {
        if (this == null) return
        (view?.parent as? ViewGroup)?.removeView(view)
        view?.text?.toString()?.let { deleteTag(it) }
    }

    override fun showUneditableAddressMessage() {
        activity?.apply { Toast.makeText(this, R.string.address_donation_services_message, Toast.LENGTH_LONG).show() }
    }
    // endregion ContactEditorCallbacks

    // region Add Person Dialog
    private fun showAddPersonDialog() = AddPersonDialogFragment()
        .show(childFragmentManager.beginTransaction().addToBackStack("AddPersonDialog"), null)

    class AddPersonDialogFragment : DialogFragment() {
        private val parent get() = parentFragment as? ContactEditorFragment

        override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.contact_add_a_person_dialog_title)
            .setMessage(R.string.contact_add_a_person_dialog_message)
            .setPositiveButton(R.string.contact_add_a_person_dialog_manually_button) { _, _ ->
                parent?.apply {
                    val person = contactViewModel.people.addModel(createPerson())?.model
                    contactViewModel.onUpdatePersonVisibility(person, true)
                }
            }
            .setNeutralButton(R.string.contact_add_a_person_dialog_from_phone_button) { _, _ ->
                parent?.showContactPicker(REQ_IMPORT_PERSON)
            }
            .create()
    }
    // endregion Add Person Dialog

    // region Unsaved Changes Dialog
    private fun setupUnsavedChangesDialog() {
        activity?.onBackPressedDispatcher?.addCallback(this, false) { showUnsavedChangesDialog() }
            ?.also { c -> contactViewModel.hasChangesLiveData.observe(this, Observer { c.isEnabled = it == true }) }
    }

    /**
     * @return true if the unsaved changes dialog was shown, false if it wasn't shown
     */
    internal fun showUnsavedChangesDialogIfNecessary(): Boolean {
        if (contactViewModel.hasChanges) {
            showUnsavedChangesDialog()
            return true
        }
        return false
    }

    private fun showUnsavedChangesDialog() = UnsavedChangesDialogFragment()
        .show(childFragmentManager.beginTransaction().addToBackStack("UnsavedChangesDialog"), null)

    class UnsavedChangesDialogFragment : DialogFragment() {
        private val parent get() = parentFragment as? ContactEditorFragment

        override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.contact_editor_unsaved_dialog)
            .setPositiveButton(R.string.contact_editor_unsaved_dialog_action_save) { _, _ -> parent?.saveContact() }
            .setNegativeButton(R.string.contact_editor_unsaved_dialog_action_discard) { _, _ -> activity?.finish() }
            .create()
    }
    // endregion Unsaved Changes Dialog
}

@HiltViewModel
class ContactEditorFragmentDataModel @Inject constructor(
    appPrefs: AppPrefs,
    private val contactsSyncService: ContactsSyncService,
    private val tagsSyncService: TagsSyncService
) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData
    val contactId = MutableLiveData<String>()

    val contact = contactId.switchMap { realm.getContact(it).firstAsLiveData() }
    val tags = accountListId.switchMap { realm.getContactTagsFor(it).asLiveData() }

    // region Sync Logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
        contactId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        contactId.value?.let { syncTracker.runSyncTasks(contactsSyncService.syncContact(it, force)) }
        accountListId.value?.let { syncTracker.runSyncTasks(tagsSyncService.syncContactTags(it, force)) }
    }
    // endregion Sync Logic
}

class FormVisibilityViewModel : ViewModel() {
    val commitment = MutableLiveData(false)
    val communication = MutableLiveData(false)
    val importContact = MutableLiveData(true)

    fun toggleCommitment() {
        commitment.value = commitment.value != true
    }

    fun toggleCommunication() {
        communication.value = communication.value != true
    }
}
