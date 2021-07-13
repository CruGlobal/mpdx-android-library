package org.mpdx.android.features.contacts.contacteditor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView.NO_ID
import org.ccci.gto.android.common.recyclerview.adapter.SimpleDataBindingAdapter
import org.ccci.gto.android.common.support.v4.util.IdUtils
import org.mpdx.android.BR
import org.mpdx.android.R
import org.mpdx.android.features.contacts.contacteditor.ContactEditorCallbacks
import org.mpdx.android.features.contacts.viewmodel.AddressViewModel
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel

private const val TYPE_ADDRESS = 2
private const val TYPE_ADD_ADDRESS = 3

internal class AddressDetailsAdapter(
    private val callbacks: ContactEditorCallbacks,
    private val contactViewModel: ContactViewModel
) : SimpleDataBindingAdapter<ViewDataBinding>() {
    var addresses: List<AddressViewModel>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private fun getAddress(position: Int) = addresses?.getOrNull(position)

    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = (addresses?.size ?: 0) + 1
    override fun getItemId(position: Int): Long {
        val addressViewModel = getAddress(position)
        if (addressViewModel != null) {
            return addressViewModel.model?.id?.let { IdUtils.convertId(it) } ?: NO_ID
        }
        return getItemViewType(position) * -1L
    }

    override fun getItemViewType(position: Int) = when {
        getAddress(position) != null -> TYPE_ADDRESS
        else -> TYPE_ADD_ADDRESS
    }

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int): ViewDataBinding {
        val layout = when (viewType) {
            TYPE_ADDRESS -> R.layout.contacts_editor_section_address_details
            TYPE_ADD_ADDRESS -> R.layout.contacts_editor_section_address_add
            else -> throw UnsupportedOperationException("Invalid ViewType - How did we get here??")
        }

        return DataBindingUtil.inflate<ViewDataBinding>(LayoutInflater.from(parent.context), layout, parent, false)
            .apply {
                setVariable(BR.callbacks, ObservableField(callbacks))
                setVariable(BR.contact, contactViewModel)
            }
    }

    override fun onBindViewDataBinding(binding: ViewDataBinding, position: Int) {
        binding.setVariable(BR.address, getAddress(position))
    }
}
