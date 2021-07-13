package org.mpdx.android.features.contacts.contactdetail.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.StringRes
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.mpdx.android.R
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.databinding.ContactsDetailsInfoFragmentBinding
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.analytics.model.ACTION_CONTACT_CALL_CLICKED
import org.mpdx.android.features.analytics.model.ACTION_CONTACT_DIRECTIONS_CLICKED
import org.mpdx.android.features.analytics.model.ACTION_CONTACT_EMAIL_CLICKED
import org.mpdx.android.features.analytics.model.ACTION_CONTACT_SMS_CLICKED
import org.mpdx.android.features.analytics.model.AnalyticsActionEvent
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.CATEGORY_CONTACTS
import org.mpdx.android.features.analytics.model.IconDirectionAnalyticsEvent
import org.mpdx.android.features.analytics.model.IconEmailAnalyticsEvent
import org.mpdx.android.features.analytics.model.SCREEN_CONTACTS_INFO
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivityViewModel
import org.mpdx.android.features.contacts.model.Address
import org.mpdx.android.features.contacts.model.EmailAddress
import org.mpdx.android.features.contacts.model.PhoneNumber
import org.mpdx.android.features.contacts.util.startMapsActivity
import org.mpdx.android.features.contacts.viewmodel.AddressViewModel
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel
import org.mpdx.android.features.tasks.taskdetail.AllowedActivityTypes
import org.mpdx.android.features.tasks.taskdetail.PhoneBottomSheetFragment
import org.mpdx.android.features.tasks.taskdetail.TaskDetailActivity
import org.mpdx.android.features.tasks.tasklist.CurrentTasksFragment
import org.mpdx.android.utils.sendEmail
import splitties.fragmentargs.argOrNull

@AndroidEntryPoint
class ContactDetailsInfoFragment() :
    DataBindingFragment<ContactsDetailsInfoFragmentBinding>(), ContactInfoViewListener {
    constructor(referringTaskId: String?, referringTaskActivityType: String?) : this() {
        this.referringTaskId = referringTaskId
        this.referringTaskActivityType = referringTaskActivityType
    }

    private var referringTaskId by argOrNull<String>()
    private var referringTaskActivityType by argOrNull<String>()

    // region Lifecycle
    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ContactsDetailsInfoFragmentBinding.inflate(inflater, container, false).also {
            it.callbacks = this
            it.contact = contactViewModel
            it.primaryAddress = primaryAddressViewModel
            it.people = dataModel.people
            it.primaryEmailAddress = dataModel.primaryEmailAddress
            it.viewModel = contactInfoViewModel
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPeopleViewPager()
    }

    override fun onResume() {
        super.onResume()
        eventBus.post(AnalyticsScreenEvent(SCREEN_CONTACTS_INFO))
    }

    override fun onDestroyView() {
        cleanupPeopleViewPager()
        super.onDestroyView()
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel: ContactDetailActivityViewModel by activityViewModels()
    // endregion Data Model

    // region View Models
    private val contactViewModel by lazy { ContactViewModel().also { dataModel.contact.observe(this, it) } }
    private val contactInfoViewModel by lazy {
        ContactInfoViewModel(this)
            .also { viewModel ->
                dataModel.people.observe(this) { viewModel.setPeople(it) }
                dataModel.contact.observe(this) { viewModel.update(it) }
            }
    }
    private val primaryAddressViewModel by lazy {
        AddressViewModel().also { dataModel.primaryAddress.observe(this, it) }
    }
    // endregion View Models

    // region People ViewPager
    private val peopleAdapter by lazy { PeopleViewPagerAdapter(this).also { dataModel.people.observe(this, it) } }

    private fun setupPeopleViewPager() {
        binding.peopleViewpager.adapter = peopleAdapter
        binding.peopleViewpager.pageMargin = 16
        binding.indicator.setViewPager(binding.peopleViewpager)
        peopleAdapter.registerDataSetObserver(binding.indicator.dataSetObserver)
    }

    private fun cleanupPeopleViewPager() {
        peopleAdapter.unregisterDataSetObserver(binding.indicator.dataSetObserver)
    }
    // endregion People ViewPager

    // region Actions
    @Inject
    lateinit var appPrefs: AppPrefs
    private inline val contact get() = dataModel.contact.value

    override fun openDirectionsTo(address: Address?) {
        if (address == null) {
            Toast.makeText(context, R.string.contact_action_no_address, LENGTH_LONG).show()
            return
        }

        eventBus.post(IconDirectionAnalyticsEvent)
        eventBus.post(AnalyticsActionEvent(ACTION_CONTACT_DIRECTIONS_CLICKED, CATEGORY_CONTACTS, CATEGORY_CONTACTS))

        onAddressClicked(address)
    }

    override fun openEmailApp(email: String?) {
        if (AllowedActivityTypes.EMAIL.getApiValue() == referringTaskActivityType || email == null) {
            restartTaskActivity()
            return
        }
        if (requireActivity().sendEmail(arrayOf(email))) {
            eventBus.post(AnalyticsActionEvent(ACTION_CONTACT_EMAIL_CLICKED, CATEGORY_CONTACTS, CATEGORY_CONTACTS))

            // log task on return
            appPrefs.lastStartedAppId = contact?.id
            appPrefs.setLastStartedApp(AllowedActivityTypes.EMAIL.apiValue)
        } else {
            Toast.makeText(requireContext(), R.string.no_email_client_found, Toast.LENGTH_SHORT).show()
        }
    }

    override fun openFacebookMessenger(url: String?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
        appPrefs.lastStartedAppId = contact?.id
        appPrefs.setLastStartedApp(AllowedActivityTypes.FACEBOOK_MESSAGE.apiValue)
    }

    override fun onAddressClicked(address: Address?) {
        address?.startMapsActivity(context)
    }

    override fun onEmailAddressClicked(emailAddress: EmailAddress?) {
        when (val email = emailAddress?.email) {
            null -> showMessageDialog(R.string.contact_action_no_email)
            else -> {
                openEmailApp(email)
                eventBus.post(IconEmailAnalyticsEvent)
            }
        }
    }

    override fun openPhoneCallApp(phoneNumbers: List<PhoneNumber>?) {
        if (AllowedActivityTypes.CALL.getApiValue() == referringTaskActivityType) {
            restartTaskActivity()
            return
        }
        if (phoneNumbers?.size == 1) {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:" + phoneNumbers[0].number)
            startActivity(intent)
        } else {
            appCompatActivity?.supportFragmentManager?.apply {
                PhoneBottomSheetFragment.newInstance(phoneNumbers, AllowedActivityTypes.CALL).show(this, null)
            }
        }
        eventBus.post(AnalyticsActionEvent(ACTION_CONTACT_CALL_CLICKED, CATEGORY_CONTACTS, CATEGORY_CONTACTS))
        appPrefs.lastStartedAppId = contact?.id
        appPrefs.setLastStartedApp(AllowedActivityTypes.CALL.apiValue)
    }

    override fun openTextMessageApp(phoneNumbers: List<PhoneNumber>?) {
        if (AllowedActivityTypes.TEXT_MESSAGE.getApiValue() == referringTaskActivityType) {
            restartTaskActivity()
            return
        }

        if (phoneNumbers?.size == 1) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("sms:" + phoneNumbers[0].number)
            startActivity(intent)
        } else {
            appCompatActivity?.supportFragmentManager?.apply {
                PhoneBottomSheetFragment.newInstance(phoneNumbers, AllowedActivityTypes.TEXT_MESSAGE).show(this, null)
            }
        }
        eventBus.post(AnalyticsActionEvent(ACTION_CONTACT_SMS_CLICKED, CATEGORY_CONTACTS, CATEGORY_CONTACTS))
        appPrefs.lastStartedAppId = contact?.id
        appPrefs.setLastStartedApp(AllowedActivityTypes.TEXT_MESSAGE.apiValue)
    }

    override fun showMessageDialog(@StringRes message: Int) {
        Toast.makeText(context, message, LENGTH_LONG).show()
    }

    private fun restartTaskActivity() {
        val intent = Intent(context, TaskDetailActivity::class.java)
        intent.putExtra(CurrentTasksFragment.TASK_ID_KEY, referringTaskId)
        intent.putExtra(TaskDetailActivity.PENDING_ACTION_KEY, referringTaskActivityType)
        startActivity(intent)
    }
    // endregion Actions
}
