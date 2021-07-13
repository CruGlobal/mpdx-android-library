package org.mpdx.android.features.appeals.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ObservableField
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.databinding.AppealsPledgesPledgeBinding
import org.mpdx.android.features.appeals.model.Pledge
import org.mpdx.android.features.appeals.viewmodel.PledgeViewModel

internal class PledgesAdapter : UniqueItemRealmDataBindingAdapter<Pledge, AppealsPledgesPledgeBinding>() {
    val listener = ObservableField<PledgeClickedListener>()

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        AppealsPledgesPledgeBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            it.listener = listener
            it.pledge = PledgeViewModel()
        }

    override fun onBindViewDataBinding(binding: AppealsPledgesPledgeBinding, position: Int) {
        binding.pledge?.model = getItem(position)
    }
}
