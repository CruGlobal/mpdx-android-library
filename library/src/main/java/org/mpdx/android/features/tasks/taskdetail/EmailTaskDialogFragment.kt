package org.mpdx.android.features.tasks.taskdetail

import android.app.Dialog
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ccci.gto.android.common.androidx.fragment.app.BaseDialogFragment
import org.ccci.gto.android.common.androidx.fragment.app.findListener
import org.ccci.gto.android.common.androidx.lifecycle.SetLiveData
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.features.contacts.model.EmailAddressFields
import org.mpdx.android.features.contacts.realm.forTask
import org.mpdx.android.features.contacts.realm.getEmailAddresses
import org.mpdx.android.features.contacts.widget.dialog.MultiSelectEmailAddressAdapter
import splitties.fragmentargs.arg

class EmailTaskDialogFragment() : BaseDialogFragment() {
    constructor(taskId: String) : this() {
        this.taskId = taskId
    }

    private var taskId by arg<String>()

    val dataModel: EmailTaskDialogDataModel by viewModels()

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataModel.taskId.value = taskId
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_multiple_email_dialog)
            .setAdapter(
                MultiSelectEmailAddressAdapter(requireContext(), dialogLifecycleOwner, dataModel.selectedEmails)
                    .also { dataModel.emailAddresses.observe(dialogLifecycleOwner, it) },
                null
            )
            .setPositiveButton(R.string.ok) { _, _ ->
                val ids = dataModel.selectedEmails.value.orEmpty()
                val emails = dataModel.emailAddresses.value?.filter { ids.contains(it.id) }?.mapNotNull { it.email }
                findListener<TaskDetailActivity>()?.sendEmail(*emails?.toTypedArray().orEmpty())
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .also { dialog ->
                dialog.listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
                dataModel.selectedEmails.observe(dialogLifecycleOwner) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = it.isNotEmpty()
                }
            }
    }
    // endregion Lifecycle
}

class EmailTaskDialogDataModel : RealmViewModel() {
    internal val taskId = MutableLiveData<String>()

    val emailAddresses = taskId.distinctUntilChanged().switchMap {
        realm.getEmailAddresses().forTask(it).distinct(EmailAddressFields.EMAIL).asLiveData()
    }

    internal val selectedEmails = SetLiveData<String>()
}
