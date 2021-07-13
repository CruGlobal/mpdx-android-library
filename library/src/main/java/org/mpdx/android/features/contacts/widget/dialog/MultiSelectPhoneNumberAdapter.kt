package org.mpdx.android.features.contacts.widget.dialog

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import org.ccci.gto.android.common.androidx.lifecycle.SetLiveData
import org.mpdx.android.R
import org.mpdx.android.base.widget.DataBindingObserverArrayAdapter
import org.mpdx.android.databinding.WidgetDialogMultiselectPhonenumberModelItemBinding
import org.mpdx.android.features.contacts.model.PhoneNumber
import org.mpdx.android.features.contacts.viewmodel.PhoneNumberViewModel

class MultiSelectPhoneNumberAdapter(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    private val selected: SetLiveData<String>
) : DataBindingObserverArrayAdapter<WidgetDialogMultiselectPhonenumberModelItemBinding, PhoneNumber>(
    context, R.layout.widget_dialog_multiselect_phonenumber_model_item, lifecycleOwner
) {
    override fun onBindingCreated(binding: WidgetDialogMultiselectPhonenumberModelItemBinding) {
        binding.selected = selected
    }

    override fun onBind(binding: WidgetDialogMultiselectPhonenumberModelItemBinding, position: Int) {
        binding.phoneNumber = (binding.phoneNumber ?: PhoneNumberViewModel()).apply { model = getItem(position) }
    }
}
