package org.mpdx.android.features.settings

import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.features.AppPrefs

@HiltViewModel
class SettingsFragmentViewModel @Inject constructor(appPrefs: AppPrefs) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData
    val accountList by lazy { accountListId.switchMap { realm.getAccountList(it).firstAsLiveData() } }
}
