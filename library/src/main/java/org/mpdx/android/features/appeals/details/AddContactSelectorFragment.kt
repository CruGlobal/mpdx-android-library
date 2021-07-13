package org.mpdx.android.features.appeals.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Case
import io.realm.kotlin.oneOf
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.filterByQuery
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.appeals.model.AppealFields
import org.mpdx.android.features.appeals.realm.forAppeal
import org.mpdx.android.features.appeals.realm.getPledges
import org.mpdx.android.features.appeals.sync.AskedContactsSyncService
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.ContactFields
import org.mpdx.android.features.contacts.realm.getContacts
import org.mpdx.android.features.contacts.realm.hasName
import org.mpdx.android.features.contacts.sync.ContactsSyncService
import org.mpdx.android.features.selector.BaseSelectorFragment
import org.mpdx.android.features.selector.SelectorFragmentDataModel
import splitties.fragmentargs.arg

@AndroidEntryPoint
class AddContactSelectorFragment() : BaseSelectorFragment<Contact>(R.string.select_contact_hint, Contact::name, true) {
    constructor(appealId: String) : this() {
        this.appealId = appealId
    }

    private var appealId by arg<String>()

    override val dataModel by lazy {
        ViewModelProvider(this).get(AddContactSelectorFragmentDataModel::class.java)
            .also { it.appealId.value = appealId }
    }
}

@HiltViewModel
class AddContactSelectorFragmentDataModel @Inject constructor(
    appPrefs: AppPrefs,
    private val contactsSyncService: ContactsSyncService,
    private val askedContactsSyncService: AskedContactsSyncService
) : RealmViewModel(), SelectorFragmentDataModel<Contact> {
    private val accountListId = appPrefs.accountListIdLiveData
    internal val appealId = MutableLiveData<String>()
    override val query = MutableLiveData("")

    private val pledges = appealId
        .switchMap { realm.getPledges().forAppeal(it).asLiveData() }
        .map { it.mapNotNull { pledge -> pledge.contact?.id } }

    override val items =
        accountListId.switchCombineWith(appealId, pledges, query) { accountListId, appealId, pledges, query ->
            realm.getContacts(accountListId)
                .hasName()
                // exclude any existing asked contacts
                .not().equalTo("${ContactFields.ASKED_CONTACTS.APPEAL}.${AppealFields.ID}", appealId)
                // exclude any existing pledges
                .not().oneOf(ContactFields.ID, pledges.orEmpty().toTypedArray(), Case.INSENSITIVE)
                .filterByQuery(ContactFields.NAME, query)
                .sort(ContactFields.NAME)
                .asLiveData()
        }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
        appealId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val accountListId = accountListId.value
        val appealId = appealId.value

        if (accountListId != null) {
            syncTracker.runSyncTasks(contactsSyncService.syncContacts(accountListId, force))
        }
        if (appealId != null) {
            syncTracker.runSyncTasks(askedContactsSyncService.syncAskedContacts(appealId, force))
        }
    }
    // endregion Sync logic
}
