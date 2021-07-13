package org.mpdx.android.features.tasks.taskdetail

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ccci.gto.android.common.androidx.fragment.app.BaseDialogFragment
import org.ccci.gto.android.common.androidx.lifecycle.SetLiveData
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.features.contacts.model.PhoneNumberFields
import org.mpdx.android.features.contacts.realm.forTask
import org.mpdx.android.features.contacts.realm.getPhoneNumbers
import org.mpdx.android.features.contacts.widget.dialog.MultiSelectPhoneNumberAdapter
import org.mpdx.android.utils.sendSms
import splitties.fragmentargs.arg

class PhoneNumberTaskDialogFragment() : BaseDialogFragment() {
    constructor(taskId: String) : this() {
        this.taskId = taskId
    }

    private var taskId by arg<String>()

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDataModel()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_phone_numbers_dialog)
            .setAdapter(
                MultiSelectPhoneNumberAdapter(requireContext(), dialogLifecycleOwner, dataModel.selectedPhoneNumbers)
                    .also { dataModel.phoneNumbers.observe(dialogLifecycleOwner, it) },
                null
            )
            .setPositiveButton(R.string.ok) { _, _ ->
                val ids = dataModel.selectedPhoneNumbers.value.orEmpty()
                val numbers = dataModel.phoneNumbers.value?.filter { ids.contains(it.id) }?.mapNotNull { it.number }
                requireContext().sendSms(numbers?.toTypedArray().orEmpty())
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .also { dialog ->
                dataModel.selectedPhoneNumbers.observe(dialogLifecycleOwner) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = it.isNotEmpty()
                }
            }
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel: PhoneNumberTaskDialogDataModel by viewModels()

    private fun setupDataModel() {
        dataModel.taskId.value = taskId
    }
    // endregion Data Model
}

class PhoneNumberTaskDialogDataModel : RealmViewModel() {
    internal val taskId = MutableLiveData<String>()

    val phoneNumbers = taskId.distinctUntilChanged().switchMap {
        realm.getPhoneNumbers().forTask(it).distinct(PhoneNumberFields.NUMBER).asLiveData()
    }
    internal val selectedPhoneNumbers = SetLiveData<String>()
}
