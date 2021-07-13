package org.mpdx.android.features.tasks.newtask

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.kotlin.oneOf
import java.util.ArrayList
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.filterByQuery
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.ContactFields
import org.mpdx.android.features.contacts.realm.getContacts
import org.mpdx.android.features.contacts.realm.hasName
import org.mpdx.android.features.contacts.sync.ContactsSyncService
import org.mpdx.android.features.selector.BaseSelectorFragment
import org.mpdx.android.features.selector.SelectorFragmentDataModel
import splitties.fragmentargs.argOrNull

@AndroidEntryPoint
class TaskContactSelectorFragment() : BaseSelectorFragment<Contact>(
    title = R.string.select_contact_hint,
    itemLabel = Contact::name,
    enableSearch = true
) {
    constructor(vararg excludedContacts: String) : this() {
        this.excludedContacts = arrayListOf(*excludedContacts)
    }

    private var excludedContacts by argOrNull<ArrayList<String>>()

    override val dataModel by lazy {
        ViewModelProvider(this).get(TaskContactSelectorFragmentDataModel::class.java)
            .also { it.excludedContacts.value = excludedContacts }
    }
}

@HiltViewModel
class TaskContactSelectorFragmentDataModel @Inject constructor(
    appPrefs: AppPrefs,
    private val contactsSyncService: ContactsSyncService
) : RealmViewModel(), SelectorFragmentDataModel<Contact> {
    private val accountListId = appPrefs.accountListIdLiveData
    val excludedContacts = MutableLiveData<List<String>?>()

    override val query = MutableLiveData("")
    override val items = accountListId.switchCombineWith(excludedContacts, query) { accountListId, excluded, query ->
        realm.getContacts(accountListId)
            .hasName()
            .apply {
                if (excluded?.isNotEmpty() == true)
                    beginGroup().not().oneOf(ContactFields.ID, excluded.toTypedArray()).endGroup()
            }
            .filterByQuery(ContactFields.NAME, query)
            .sort(ContactFields.NAME)
            .asLiveData()
    }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val accountListId = accountListId.value ?: return
        syncTracker.runSyncTasks(contactsSyncService.syncContacts(accountListId, force))
    }
    // endregion Sync logic
}
