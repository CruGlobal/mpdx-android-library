package org.mpdx.android.features.contacts.list

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import io.realm.kotlin.oneOf
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.emptyLiveData
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.ccci.gto.android.common.recyclerview.util.lifecycleOwner
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.core.modal.ModalFragment
import org.mpdx.android.databinding.ContactsListByGroupingFragmentBinding
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.base.fragments.BindingFragment
import org.mpdx.android.features.constants.model.ConstantList.Companion.STATUS_PARTNER_FINANCIAL
import org.mpdx.android.features.contacts.ContactClickListener
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivity
import org.mpdx.android.features.contacts.list.ContactsGrouping.ANNIVERSARIES_THIS_WEEK
import org.mpdx.android.features.contacts.list.ContactsGrouping.GIFTS_NOT_RECEIVED
import org.mpdx.android.features.contacts.list.ContactsGrouping.PARTNERS_30_DAYS_LATE
import org.mpdx.android.features.contacts.list.ContactsGrouping.PARTNERS_60_DAYS_LATE
import org.mpdx.android.features.contacts.list.ContactsGrouping.PARTNERS_ALL_DAYS_LATE
import org.mpdx.android.features.contacts.list.ContactsGrouping.REFERRALS
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.Contact.DonationLateState
import org.mpdx.android.features.contacts.model.ContactFields
import org.mpdx.android.features.contacts.realm.donationLateState
import org.mpdx.android.features.contacts.realm.getContacts
import org.mpdx.android.features.contacts.realm.hasName
import org.mpdx.android.features.contacts.realm.receivedGift
import org.mpdx.android.features.contacts.realm.sortByName
import org.mpdx.android.features.contacts.realm.status
import org.mpdx.android.features.contacts.repository.ContactsRepository
import splitties.fragmentargs.arg

@AndroidEntryPoint
class ContactsByGroupingFragment() :
    BindingFragment<ContactsListByGroupingFragmentBinding>(),
    ModalFragment,
    ContactClickListener {
    constructor(grouping: ContactsGrouping, vararg contactIds: String) : this() {
        this.grouping = grouping
        @Suppress("UNCHECKED_CAST")
        this.contactIds = contactIds as Array<String>
    }

    @Inject
    internal lateinit var contactsRepository: ContactsRepository

    private var grouping by arg<ContactsGrouping>()
    private var contactIds by arg<Array<String>>()

    // region Lifecycle
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDataBinding()
        setupContactsList()
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel by lazy {
        ViewModelProvider(this).get(ContactsByGroupingFragmentDataModel::class.java).also {
            it.grouping.value = grouping
            it.contactIds.value = contactIds
        }
    }
    // endregion Data Model

    // region Data Binding
    private fun setupDataBinding() {
        binding.grouping = grouping
    }
    // endregion Data Binding

    // region Contacts List
    private val adapter by lazy {
        ContactsAdapter(contactsRepository, false).also {
            it.contactClickListener.set(this)
            dataModel.contacts.observe(this, it)
        }
    }

    private fun setupContactsList() {
        binding.contacts.also {
            it.lifecycleOwner = viewLifecycleOwner
            it.adapter = adapter
        }
    }
    // endregion Contacts List

    // region ContactClickListener
    override fun onContactClick(contact: Contact?) {
        contact?.id?.let { startActivity(ContactDetailActivity.getIntent(requireActivity(), it)) }
    }
    // endregion ContactClickListener

    override fun getToolbar() = binding.toolbar
    override fun layoutRes() = R.layout.contacts_list_by_grouping_fragment
}

@HiltViewModel
class ContactsByGroupingFragmentDataModel @Inject constructor(appPrefs: AppPrefs) : RealmViewModel() {
    private val accountListId = appPrefs.accountListIdLiveData
    val grouping = MutableLiveData<ContactsGrouping?>(null)
    val contactIds = MutableLiveData<Array<String>?>(null)

    val contacts by lazy {
        accountListId.switchCombineWith(grouping, contactIds) { accountListId, grouping, contactIds ->
            realm.getContacts(accountListId).hasName().sortByName().apply {
                when (grouping) {
                    ANNIVERSARIES_THIS_WEEK, REFERRALS -> oneOf(ContactFields.ID, contactIds.orEmpty())
                    GIFTS_NOT_RECEIVED -> status(STATUS_PARTNER_FINANCIAL).receivedGift(false)
                    PARTNERS_30_DAYS_LATE ->
                        status(STATUS_PARTNER_FINANCIAL).donationLateState(DonationLateState.THIRTY_DAYS_LATE)
                    PARTNERS_60_DAYS_LATE ->
                        status(STATUS_PARTNER_FINANCIAL).donationLateState(DonationLateState.SIXTY_DAYS_LATE)
                    PARTNERS_ALL_DAYS_LATE ->
                        status(STATUS_PARTNER_FINANCIAL).donationLateState(DonationLateState.ALL_LATE)
                    else -> return@switchCombineWith emptyLiveData<RealmResults<Contact>>()
                }
            }.asLiveData()
        }
    }
}

enum class ContactsGrouping(@StringRes val title: Int) {
    GIFTS_NOT_RECEIVED(R.string.gifts_not_received_label),
    PARTNERS_30_DAYS_LATE(R.string.partners_thirty_days_late_label),
    PARTNERS_60_DAYS_LATE(R.string.partners_sixty_days_late_label),
    PARTNERS_ALL_DAYS_LATE(R.string.partners_all_late_label),
    DAYS_SINCE_LAST_NEWSLETTER(R.string.days_since_last_newsletter_label),
    ANNIVERSARIES_THIS_WEEK(R.string.anniversaries_this_week_label),
    REFERRALS(R.string.dashboard_referrals)
}
