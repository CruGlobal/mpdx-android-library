package org.mpdx.android.features.contacts.people

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ObservableField
import org.mpdx.android.BR
import org.mpdx.android.base.widget.recyclerview.UniqueItemDataBindingAdapter
import org.mpdx.android.databinding.PeopleListItemBirthdayBinding
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.viewmodel.PersonViewModel

internal class PeopleByGroupingAdapter : UniqueItemDataBindingAdapter<Person, PeopleListItemBirthdayBinding>() {
    val personSelectedListener = ObservableField<PersonSelectedListener>()

    // region Lifecycle
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        PeopleListItemBirthdayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            .also { it.selectedListener = personSelectedListener }

    override fun onBindViewDataBinding(binding: PeopleListItemBirthdayBinding, position: Int) {
        binding.setVariable(BR.person, PersonViewModel().apply { model = getItem(position) })
    }
    // endregion Lifecycle
}
