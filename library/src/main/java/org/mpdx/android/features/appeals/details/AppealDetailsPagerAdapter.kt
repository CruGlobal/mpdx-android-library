package org.mpdx.android.features.appeals.details

import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.mpdx.android.R
import org.mpdx.android.features.appeals.details.asked.AskedContactsFragment
import org.mpdx.android.features.appeals.details.committed.CommittedContactsFragment
import org.mpdx.android.features.appeals.details.given.CommitmentsGivenFragment
import org.mpdx.android.features.appeals.details.received.CommitmentsReceivedFragment

class AppealDetailsPagerAdapter(private val context: Context, fragmentManager: FragmentManager) :
    FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    enum class Pages(@StringRes val title: Int) {
        ASKED(R.string.appeals_asked),
        COMMITTED(R.string.appeals_committed),
        RECEIVED(R.string.appeals_received),
        GIVEN(R.string.appeals_given)
    }

    override fun getCount() = Pages.values().size
    override fun getItem(position: Int) = when (Pages.values()[position]) {
        Pages.ASKED -> AskedContactsFragment()
        Pages.COMMITTED -> CommittedContactsFragment()
        Pages.RECEIVED -> CommitmentsReceivedFragment()
        Pages.GIVEN -> CommitmentsGivenFragment()
    }

    override fun getPageTitle(position: Int) = context.getString(Pages.values()[position].title)
}
