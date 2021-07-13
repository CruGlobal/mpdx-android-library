package org.mpdx.android.features.tasks.editor

import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Sort
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.ccci.gto.android.common.util.findListener
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.core.model.User
import org.mpdx.android.core.model.UserFields
import org.mpdx.android.core.realm.forAccountList
import org.mpdx.android.core.realm.getUsers
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.selector.BaseSelectorFragment
import org.mpdx.android.features.selector.SelectorFragmentDataModel

@AndroidEntryPoint
class TaskUserSelectorFragment : BaseSelectorFragment<User>(
    title = R.string.select_user_hint,
    itemLabel = { "$firstName $lastName" },
    enableSearch = false
) {
    override val dataModel: TaskUserSelectorFragmentDataModel by viewModels()

    override fun dispatchItemSelectedCallback(item: User?) {
        findListener<OnUserSelectedListener>()?.onUserSelected(item)
    }
}

@HiltViewModel
class TaskUserSelectorFragmentDataModel @Inject constructor(appPrefs: AppPrefs) :
    RealmViewModel(), SelectorFragmentDataModel<User> {
    private val accountListId = appPrefs.accountListIdLiveData

    override val query = MutableLiveData("")
    override val items = accountListId.switchCombineWith(query) { accountListId, query ->
        realm.getUsers().forAccountList(accountListId)
            .sort(UserFields.FIRST_NAME, Sort.ASCENDING, UserFields.LAST_NAME, Sort.ASCENDING)
            .asLiveData()
    }
}

interface OnUserSelectedListener {
    fun onUserSelected(user: User?)
}
