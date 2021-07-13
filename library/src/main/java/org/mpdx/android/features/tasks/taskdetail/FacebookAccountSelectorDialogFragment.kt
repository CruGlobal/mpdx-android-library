package org.mpdx.android.features.tasks.taskdetail

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.ccci.gto.android.common.util.findListener
import org.mpdx.android.R
import org.mpdx.android.features.contacts.model.FacebookAccount
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.utils.realm
import splitties.fragmentargs.arg

class FacebookAccountSelectorDialogFragment() : DialogFragment() {
    constructor(contactId: String) : this() {
        this.contactId = contactId
    }

    private var contactId by arg<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val facebookAccounts = getFacebookAccount(contactId)
        val userNameList = facebookAccounts.mapNotNull { it.username }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_facebook_account)
            .setItems(userNameList.toTypedArray()) { _, which ->
                findListener<FacebookAccountClickedListener>()?.onFacebookAccountClicked(facebookAccounts[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    private fun getFacebookAccount(contactId: String?): List<FacebookAccount> {
        return realm {
            getContact(contactId).findFirst()?.getPeople()?.findAll()
                ?.flatMap { it.getFacebookAccounts()?.findAll().orEmpty() }.orEmpty()
        }
    }
}

interface FacebookAccountClickedListener {
    fun onFacebookAccountClicked(account: FacebookAccount?)
}
