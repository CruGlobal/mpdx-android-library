package org.mpdx.android.features.contacts.contactdetail

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentPagerAdapter
import org.mpdx.android.features.contacts.ContactsPagerEnum
import org.mpdx.android.features.contacts.ContactsPagerEnum.DONATION
import org.mpdx.android.features.contacts.ContactsPagerEnum.INFO
import org.mpdx.android.features.contacts.ContactsPagerEnum.NOTES
import org.mpdx.android.features.contacts.ContactsPagerEnum.TASKS
import org.mpdx.android.features.contacts.contactdetail.donation.ContactDetailDontationFragment
import org.mpdx.android.features.contacts.contactdetail.info.ContactDetailsInfoFragment
import org.mpdx.android.features.contacts.contactdetail.notes.ContactDetailNotesFragment
import org.mpdx.android.features.contacts.contactdetail.tasks.ContactDetailTaskFragment

class ContactDetailFragmentPagerAdapter(
    val activity: FragmentActivity,
    private val referringTaskId: String?,
    private val referringTaskActivityType: String?
) : FragmentPagerAdapter(activity.supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        return when (ContactsPagerEnum.values()[position]) {
            INFO -> ContactDetailsInfoFragment(referringTaskId, referringTaskActivityType)
            DONATION -> ContactDetailDontationFragment()
            NOTES -> ContactDetailNotesFragment()
            TASKS -> ContactDetailTaskFragment()
        }
    }

    override fun getCount(): Int = ContactsPagerEnum.values().size

    override fun getPageTitle(position: Int): CharSequence? {
        return activity.getString(ContactsPagerEnum.values()[position].titleResId)
    }
}
