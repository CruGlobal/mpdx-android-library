package org.mpdx.android.features.appeals

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.orEmpty
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.filterByQuery
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.appeals.model.AskedContact
import org.mpdx.android.features.appeals.model.AskedContactFields
import org.mpdx.android.features.appeals.realm.getAppeal
import org.mpdx.android.features.appeals.realm.hasName
import org.mpdx.android.features.appeals.realm.sortByName
import org.mpdx.android.features.appeals.sync.AppealsSyncService
import org.mpdx.android.features.appeals.sync.AskedContactsSyncService
import org.mpdx.android.features.selector.BaseSelectorFragment
import org.mpdx.android.features.selector.SelectorFragmentDataModel
import splitties.fragmentargs.arg

@AndroidEntryPoint
class AskedContactSelectorFragment() :
    BaseSelectorFragment<AskedContact>(R.string.select_contact_hint, { contact?.name }, true) {
    constructor(appealId: String) : this() {
        this.appealId = appealId
    }

    private var appealId by arg<String>()

    override val dataModel by lazy {
        ViewModelProvider(this).get(AskedContactSelectorFragmentDataModel::class.java)
            .also { it.appealId.value = appealId }
    }
}

@HiltViewModel
class AskedContactSelectorFragmentDataModel @Inject constructor(
    private val appealsSyncService: AppealsSyncService,
    private val askedContactsSyncService: AskedContactsSyncService
) : RealmViewModel(), SelectorFragmentDataModel<AskedContact> {
    internal val appealId = MutableLiveData<String?>()
    override val query = MutableLiveData("")

    private val appeal = appealId.distinctUntilChanged().switchMap { realm.getAppeal(it).firstAsLiveData() }
    override val items = appeal.switchCombineWith(query) { appeal, query ->
        appeal?.getAskedContacts()
            ?.hasName()
            ?.filterByQuery(AskedContactFields.CONTACT.NAME, query)
            ?.sortByName()
            ?.asLiveData()
            .orEmpty()
    }

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        appealId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val appealId = appealId.value ?: return
        syncTracker.runSyncTasks(
            appealsSyncService.syncAppeal(appealId, force),
            askedContactsSyncService.syncAskedContacts(appealId, force)
        )
    }
    // endregion Sync logic
}
