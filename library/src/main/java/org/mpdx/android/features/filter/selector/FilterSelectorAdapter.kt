package org.mpdx.android.features.filter.selector

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ObservableField
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.databinding.FiltersSelectorFilterBinding
import org.mpdx.android.features.filter.model.Filter

class FilterSelectorAdapter : UniqueItemRealmDataBindingAdapter<Filter, FiltersSelectorFilterBinding>() {
    val filterActions = ObservableField<FilterActions>()

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        FiltersSelectorFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            .also { it.filterActions = filterActions }

    override fun onBindViewDataBinding(binding: FiltersSelectorFilterBinding, position: Int) {
        binding.filter = getItem(position)
    }
}
