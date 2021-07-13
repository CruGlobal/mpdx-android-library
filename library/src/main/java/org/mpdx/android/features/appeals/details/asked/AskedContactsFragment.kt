package org.mpdx.android.features.appeals.details.asked

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.ccci.gto.android.common.recyclerview.util.lifecycleOwner
import org.ccci.gto.android.common.util.findListener
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.databinding.AppealsAskedViewItemBinding
import org.mpdx.android.databinding.AppealsDetailsPageAskedBinding
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_APPEALS_ASKED
import org.mpdx.android.features.appeals.details.AddCommitmentListener
import org.mpdx.android.features.appeals.details.AppealDetailsActivityDataModel
import org.mpdx.android.features.appeals.model.AskedContact

@AndroidEntryPoint
class AskedContactsFragment : DataBindingFragment<AppealsDetailsPageAskedBinding>(), AddCommitmentListener {
    // region Lifecycle
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        AppealsDetailsPageAskedBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAskedContactsList()
    }

    override fun onResume() {
        super.onResume()
        eventBus.post(AnalyticsScreenEvent(SCREEN_APPEALS_ASKED))
    }
    // endregion Lifecycle

    private val dataModel: AppealDetailsActivityDataModel by activityViewModels()

    // region Asked Contacts List
    private val adapter by lazy {
        AskedContactsAdapter().also {
            dataModel.askedContacts.observe(viewLifecycleOwner, it)
            it.callbacks.set(this)
        }
    }

    private fun setupAskedContactsList() {
        binding.askedContacts.also {
            it.lifecycleOwner = viewLifecycleOwner
            it.adapter = adapter
        }
    }
    // endregion Asked Contacts List

    override fun onAddCommitment(contact: AskedContact?) {
        findListener<AddCommitmentListener>()?.onAddCommitment(contact)
    }
}

class AskedContactsAdapter : UniqueItemRealmDataBindingAdapter<AskedContact, AppealsAskedViewItemBinding>() {
    val callbacks = ObservableField<AddCommitmentListener>()

    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        AppealsAskedViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            .also { it.callbacks = callbacks }

    override fun onBindViewDataBinding(binding: AppealsAskedViewItemBinding, position: Int) {
        binding.contact = getItem(position)
    }
}
