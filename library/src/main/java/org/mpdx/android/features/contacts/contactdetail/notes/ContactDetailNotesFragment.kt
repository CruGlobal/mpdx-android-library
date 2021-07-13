package org.mpdx.android.features.contacts.contactdetail.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.mpdx.android.R
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.databinding.ContactsDetailsPageNotesBinding
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivityViewModel
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.features.contacts.sync.ContactsSyncService
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel
import org.mpdx.android.utils.hideKeyboard
import org.mpdx.android.utils.realmTransactionAsync
import org.mpdx.android.utils.showKeyboard

@AndroidEntryPoint
class ContactDetailNotesFragment @Inject constructor() :
    DataBindingFragment<ContactsDetailsPageNotesBinding>(), ContactNotesViewListener {
    @Inject
    lateinit var contactsSyncService: ContactsSyncService

    // region Lifecycle
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ContactsDetailsPageNotesBinding.inflate(inflater, container, false).also {
            it.callbacks = this
            it.contact = contactViewModel
            it.viewModel = viewModel
        }
    // endregion Lifecycle

    // region Data Model
    private val dataModel: ContactDetailActivityViewModel by activityViewModels()
    private val viewModel: ContactDetailsNotesFragmentViewModel by viewModels()

    private val contactViewModel by lazy {
        ViewModelProvider(this).get(ContactViewModel::class.java).apply {
            allowNullModel = false
            forceUnmanaged = true
            trackingChanges = true
        }.also { dataModel.contact.observe(this, it) }
    }
    // endregion Data Model

    private fun setEditing(state: Boolean) {
        viewModel.isEditing.set(state)
        if (state) {
            binding.contactNoteEditable.requestFocus()
            binding.contactNoteEditable.showKeyboard()
        } else {
            binding.contactNoteEditable.hideKeyboard()
        }
    }

    override fun onNotesClicked() {
        setEditing(true)
    }

    override fun onNotesSaved() {
        val contactId = dataModel.contactId.value
        realmTransactionAsync(onSuccess = contactsSyncService.syncDirtyContacts()::launch) {
            getContact(contactId).findFirst()?.let { contact ->
                contact.trackingChanges = true
                contact.notes = contactViewModel.notes
            }
        }
        setEditing(false)
    }

    override fun onNotesCanceled() {
        Toast.makeText(context, R.string.notes_canceled, Toast.LENGTH_SHORT).show()
        contactViewModel.notes = dataModel.contact.value?.notes
        contactViewModel.model = dataModel.contact.value
        setEditing(false)
    }
}

internal class ContactDetailsNotesFragmentViewModel : ViewModel() {
    val isEditing = ObservableBoolean(false)
}
