package org.mpdx.android.features.donations.add

import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.donations.realm.getDesignationAccounts
import org.mpdx.android.features.donations.realm.sortByName
import org.mpdx.android.features.donations.realm.withName

@HiltViewModel
class AddDonationFragmentDataModel @Inject constructor(appPrefs: AppPrefs) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData
    val designationAccounts =
        accountListId.switchMap { realm.getDesignationAccounts(it).withName().sortByName().asLiveData() }
}
