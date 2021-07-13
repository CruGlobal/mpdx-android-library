package org.mpdx.android.features.contacts.widget.dialog

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import org.ccci.gto.android.common.androidx.lifecycle.SetLiveData
import org.mpdx.android.R
import org.mpdx.android.base.widget.DataBindingObserverArrayAdapter
import org.mpdx.android.databinding.WidgetDialogMultiselectEmailModelItemBinding
import org.mpdx.android.features.contacts.model.EmailAddress
import org.mpdx.android.features.contacts.viewmodel.EmailAddressViewModel

class MultiSelectEmailAddressAdapter(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    private val selected: SetLiveData<String>
) : DataBindingObserverArrayAdapter<WidgetDialogMultiselectEmailModelItemBinding, EmailAddress>(
    context, R.layout.widget_dialog_multiselect_email_model_item, lifecycleOwner
) {
    override fun onBindingCreated(binding: WidgetDialogMultiselectEmailModelItemBinding) {
        binding.selected = selected
    }

    override fun onBind(binding: WidgetDialogMultiselectEmailModelItemBinding, position: Int) {
        binding.email = (binding.email ?: EmailAddressViewModel()).apply { model = getItem(position) }
    }
}
