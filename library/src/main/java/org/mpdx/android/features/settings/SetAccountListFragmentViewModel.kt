package org.mpdx.android.features.settings

import androidx.lifecycle.LiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import javax.inject.Inject
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.core.realm.getAccountLists

@HiltViewModel
class SetAccountListFragmentViewModel @Inject constructor() : RealmViewModel() {
    val allAccountList: LiveData<RealmResults<AccountList>> by lazy {
        realm.getAccountLists().asLiveData()
    }
}
