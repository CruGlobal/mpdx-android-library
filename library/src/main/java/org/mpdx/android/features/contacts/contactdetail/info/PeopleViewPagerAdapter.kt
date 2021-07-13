package org.mpdx.android.features.contacts.contactdetail.info

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.h6ah4i.android.widget.advrecyclerview.composedadapter.ComposedAdapter
import org.ccci.gto.android.common.viewpager.adapter.BaseDataBindingPagerAdapter
import org.ccci.gto.android.common.viewpager.adapter.DataBindingViewHolder
import org.mpdx.android.ContactPeopleViewBinding
import org.mpdx.android.R
import org.mpdx.android.features.contacts.contactdetail.info.adapter.FacebookAccountAdapter
import org.mpdx.android.features.contacts.contactdetail.info.adapter.LinkedInAdapter
import org.mpdx.android.features.contacts.contactdetail.info.adapter.TwitterAdapter
import org.mpdx.android.features.contacts.contactdetail.info.adapter.WebsiteAdapter
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.model.PhoneNumber
import org.mpdx.android.features.contacts.viewmodel.PersonViewModel

class PeopleViewPagerAdapter(private val listener: ContactInfoViewListener) :
    BaseDataBindingPagerAdapter<ContactPeopleViewBinding, PeopleViewPagerAdapter.PeopleViewHolder>(),
    Observer<List<Person>?> {
    private var people: List<Person>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getCount() = people?.size ?: 0

    override fun onChanged(t: List<Person>?) {
        people = t
    }

    override fun onCreateViewHolder(parent: ViewGroup): PeopleViewHolder =
        PeopleViewHolder(
            ContactPeopleViewBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            listener
        )

    override fun onBindViewDataBinding(holder: PeopleViewHolder, binding: ContactPeopleViewBinding, position: Int) {
        holder.apply {
            personViewModel.model = people?.get(position)
            viewModel.emailAddresses = personViewModel.emailAddresses.models
            viewModel.phoneNumbers = personViewModel.phoneNumbers.models

            facebookAccountAdapter.items = personViewModel.facebookAccounts.models
            linkedInAdapter.items = personViewModel.linkedInAccounts.models
            twitterAdapter.items = personViewModel.twitterAccounts.models
            websiteAdapter.items = personViewModel.websites.models

            emailAdapter.items = personViewModel.emailAddresses.models
            phoneNumberAdapter.items = personViewModel.phoneNumbers.models
        }
    }

    class PeopleViewHolder(binding: ContactPeopleViewBinding, listener: ContactInfoViewListener) :
        DataBindingViewHolder<ContactPeopleViewBinding>(binding) {
        val emailAdapter = ContactPersonEmailAdapter(object : ContactPersonEmailAdapter.EmailSelectedListener {
            override fun onEmailSelected(emailAddress: String?) {
                listener.openEmailApp(emailAddress)
            }
        })
        val phoneNumberAdapter =
            ContactPersonPhoneNumberAdapter(object : ContactPersonPhoneNumberAdapter.PhoneCallListener {
                override fun onPhoneCallSelected(phoneNumber: PhoneNumber) {
                    AlertDialog.Builder(binding.root.context)
                        .setTitle(R.string.call_or_text)
                        .setMessage(binding.root.context.getString(R.string.call_or_text_number, phoneNumber.number))
                        .setPositiveButton(R.string.call) { _, _ -> listener.openPhoneCallApp(listOf(phoneNumber)) }
                        .setNegativeButton(R.string.text) { _, _ -> listener.openTextMessageApp(listOf(phoneNumber)) }
                        .show()
                }
            })

        val personViewModel = PersonViewModel()
        val linkedInAdapter = LinkedInAdapter()
        val twitterAdapter = TwitterAdapter()
        val websiteAdapter = WebsiteAdapter()
        val facebookAccountAdapter = FacebookAccountAdapter(object : FacebookAccountAdapter.FacebookSelectListener {
            override fun openFacebookMessenger(username: String) {
                listener.openFacebookMessenger(String.format("http://m.me/%s", username))
            }
        })

        val viewModel =
            ContactInfoPeopleViewModel()

        init {
            binding.person = personViewModel
            binding.viewModel = viewModel

            binding.contactEmailRecyclerView.adapter = emailAdapter
            binding.contactPhoneNumberRecyclerView.adapter = phoneNumberAdapter
            binding.contactSocialMediaRecyclerView.adapter = ComposedAdapter().apply {
                addAdapter(facebookAccountAdapter)
                addAdapter(linkedInAdapter)
                addAdapter(twitterAdapter)
                addAdapter(websiteAdapter)
            }
        }
    }
}
