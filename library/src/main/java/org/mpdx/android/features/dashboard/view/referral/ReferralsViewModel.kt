package org.mpdx.android.features.dashboard.view.referral

import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.contacts.realm.forAccountList
import org.mpdx.android.features.contacts.realm.getContacts
import org.mpdx.android.features.contacts.realm.isReferral

@HiltViewModel
class ReferralsViewModel @Inject constructor(appPrefs: AppPrefs) : RealmViewModel() {
    private val accountListId = appPrefs.accountListIdLiveData

    val allReferralContacts = accountListId.switchMap {
        realm.getContacts().forAccountList(it).isReferral().asLiveData()
    }

    val numberOfReferrals = allReferralContacts.map { it.count() }

    val firstThreeReferralContacts = allReferralContacts.switchMap {
        it.where().limit(3).asLiveData()
    }
}
