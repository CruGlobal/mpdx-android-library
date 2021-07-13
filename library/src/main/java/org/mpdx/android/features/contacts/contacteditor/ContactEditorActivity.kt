package org.mpdx.android.features.contacts.contacteditor

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import org.mpdx.android.R
import org.mpdx.android.base.activity.BaseActivity
import org.mpdx.android.base.activity.DataBindingActivity
import org.mpdx.android.databinding.ContactsEditorActivityBinding

private const val ARG_CONTACT = "contact_id"

@JvmOverloads
fun Activity.startContactEditorActivity(contactId: String? = null) = startActivity(newContactEditorIntent(contactId))
fun Context.newContactEditorIntent(contactId: String? = null) = Intent(this, ContactEditorActivity::class.java).apply {
    putExtra(ARG_CONTACT, contactId)
}

@AndroidEntryPoint
class ContactEditorActivity : BaseActivity(), DataBindingActivity<ContactsEditorActivityBinding> {
    private inline val contactId get() = intent?.getStringExtra(ARG_CONTACT)

    // region Lifecycle
    override fun onContentChanged() {
        super.onContentChanged()
        setupToolbar()
        createFragmentIfNeeded()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (contactEditorFragment?.showUnsavedChangesDialogIfNecessary() != true) {
            // we just suppress native up navigation for a simpler finish() implementation
            finish()
        }
        return true
    }
    // endregion Lifecycle

    // region Data Binding
    override fun layoutId() = R.layout.contacts_editor_activity
    override lateinit var binding: ContactsEditorActivityBinding

    override fun onCreateDataBinding(binding: ContactsEditorActivityBinding) {
        super.onCreateDataBinding(binding)
        binding.contactId = contactId
    }
    // endregion Data Binding

    // region ContactEditorFragment
    private val contactEditorFragment
        get() = supportFragmentManager.primaryNavigationFragment as? ContactEditorFragment

    private fun createFragmentIfNeeded() {
        if (contactEditorFragment != null) return

        val fragment = ContactEditorFragment(contactId)
        supportFragmentManager.commit {
            setPrimaryNavigationFragment(fragment)
            replace(R.id.frame, fragment)
        }
    }
    // endregion ContactEditorFragment

    override fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        super.setupToolbar()
    }

    override fun getPageName() = "ContactEditor"
}
