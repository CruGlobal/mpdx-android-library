package org.mpdx.android.features.contacts.contactdetail.donation

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import dagger.hilt.android.AndroidEntryPoint
import org.ccci.gto.android.common.recyclerview.util.lifecycleOwner
import org.mpdx.android.R
import org.mpdx.android.R2
import org.mpdx.android.core.modal.ModalActivity
import org.mpdx.android.core.modal.ModalFragment
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_DONATIONS_HISTORY
import org.mpdx.android.features.base.fragments.BaseFragment
import org.mpdx.android.features.donations.add.AddDonationFragment
import org.mpdx.android.features.donations.list.DonationsFragment
import org.mpdx.android.features.donations.model.Donation
import splitties.fragmentargs.arg

@AndroidEntryPoint
class ContactDonationsFragment() :
    BaseFragment(),
    ModalFragment,
    ContactDonationAdapterListener {
    constructor(contactId: String) : this() {
        this.contactId = contactId
    }

    @JvmField
    @BindView(R2.id.contact_donations_toolbar)
    internal var toolbar: Toolbar? = null

    private var contactId by arg<String>()

    // region Lifecycle Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDataModel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        mEventBus.post(AnalyticsScreenEvent(SCREEN_CONTACTS_DONATIONS_HISTORY))
    }
    // endregion Lifecycle Events

    // region Data Model
    private val dataModel: ContactDonationsFragmentViewModel by viewModels()

    private fun initDataModel() {
        dataModel.contactId.value = contactId
    }
    // endregion Data Model

    // region RecyclerView

    @JvmField
    @BindView(R2.id.contact_donations_recycler)
    internal var recyclerView: RecyclerView? = null

    private val adapter: ContactDonationAdapter by lazy {
        ContactDonationAdapter(this).also { dataModel.donations.observe(this, it) }
    }

    private fun setupRecyclerView() {
        recyclerView?.also {
            it.lifecycleOwner = viewLifecycleOwner
            it.adapter = adapter
        }
    }
    // endregion RecyclerView

    override fun getToolbar() = toolbar
    override fun layoutRes() = R.layout.fragment_contact_donations

    override fun onDonationClicked(donation: Donation) {
        val fragment = AddDonationFragment.newInstance(donation.id)
        ModalActivity.launchActivityForResult(requireActivity(), fragment, DonationsFragment.REQUEST_EDIT_DONATION)
    }

    override fun onViewMoreDonationsClicked() = Unit
}
