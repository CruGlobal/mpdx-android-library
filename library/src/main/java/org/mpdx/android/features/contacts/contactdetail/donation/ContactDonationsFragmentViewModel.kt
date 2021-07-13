package org.mpdx.android.features.contacts.contactdetail.donation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.features.donations.realm.forContact
import org.mpdx.android.features.donations.realm.getDonations
import org.mpdx.android.features.donations.realm.sortByDate
import org.mpdx.android.features.donations.sync.DonationsSyncService

@HiltViewModel
class ContactDonationsFragmentViewModel @Inject constructor(
    private val donationsSyncService: DonationsSyncService
) : RealmViewModel() {
    val contactId = MutableLiveData<String?>()

    private val contact = contactId.switchMap { realm.getContact(it).firstAsLiveData() }
    val donations by lazy { contactId.switchMap { realm.getDonations().forContact(it).sortByDate().asLiveData() } }

    // region Sync logic
    private val syncTracker = SyncTracker()

    init {
        contact.observe(this) { syncData() }
    }

    private fun syncData(force: Boolean = false) {
        val contact = contact.value ?: return
        val accountListId = contact.accountList?.id ?: return
        val donorAccounts = contact.donorAccountIds?.takeUnless { it.isEmpty() } ?: return
        syncTracker.runSyncTask(
            donationsSyncService.syncDonationsForDonorAccounts(accountListId, force, *donorAccounts.toTypedArray())
        )
    }
    // endregion Sync logic
}
