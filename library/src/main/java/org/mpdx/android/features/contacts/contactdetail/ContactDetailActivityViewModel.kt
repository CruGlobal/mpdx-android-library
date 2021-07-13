package org.mpdx.android.features.contacts.contactdetail

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Sort
import io.realm.kotlin.oneOf
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.combineWith
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.contacts.realm.forContact
import org.mpdx.android.features.contacts.realm.getAddresses
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.features.contacts.realm.getEmailAddresses
import org.mpdx.android.features.contacts.realm.getPeople
import org.mpdx.android.features.contacts.realm.sortByPrimary
import org.mpdx.android.features.contacts.sync.ContactsSyncService
import org.mpdx.android.features.tasks.model.TaskFields
import org.mpdx.android.features.tasks.realm.completed
import org.mpdx.android.features.tasks.realm.forContact
import org.mpdx.android.features.tasks.realm.getTasks
import org.mpdx.android.features.tasks.sync.TasksSyncService
import org.mpdx.android.utils.CurrencyFormatter
import org.mpdx.android.utils.StringResolver

const val MAX_GIFTS_VISIBLE = 6

@HiltViewModel
class ContactDetailActivityViewModel @Inject constructor(
    appPrefs: AppPrefs,
    private val contactsSyncService: ContactsSyncService,
    private val tasksSyncService: TasksSyncService,
    private val stringResolver: StringResolver,
    private val currencyFormatter: CurrencyFormatter,
) : RealmViewModel() {
    val contactId = MutableLiveData<String?>()
    private val distinctContactId = contactId.distinctUntilChanged()

    val contact = distinctContactId.switchMap { realm.getContact(it).firstAsLiveData() }
    val addresses by lazy {
        distinctContactId.switchMap { realm.getAddresses().forContact(it).sortByPrimary().asLiveData() }
    }
    val primaryAddress by lazy { addresses.switchMap { it.firstAsLiveData() } }
    val people by lazy { distinctContactId.switchMap { realm.getPeople().forContact(it).asLiveData() } }
    val primaryEmailAddress = distinctContactId.switchMap { realm.getEmailAddresses().forContact(it).firstAsLiveData() }

    private val accountListId = contact.combineWith(appPrefs.accountListIdLiveData) { contact, accountListId ->
        contact?.accountList?.id ?: accountListId
    }.distinctUntilChanged()

    val hiddenTasks = MutableLiveData<MutableCollection<String?>>(mutableListOf())
    val tasks = accountListId.switchCombineWith(distinctContactId, hiddenTasks) { accountListId, contactId, hidden ->
        realm.getTasks(accountListId).forContact(contactId)
            .completed(false)
            .apply {
                if (!hidden.isNullOrEmpty())
                    beginGroup().not().oneOf(TaskFields.ID, hidden.toTypedArray()).endGroup()
            }
            .sort(TaskFields.START_AT, Sort.DESCENDING)
            .asLiveData()
    }

    val contactNotFound: LiveData<Boolean> by lazy {
        contact.combineWith(syncTracker.isSyncing) { contact, syncing ->
            contact == null && !syncing && syncTracker.initialSyncFinished
        }
    }

    // region For ContactDetailDonationFragment

    private val pledgeAmount = contact.map { it?.pledgeAmount }
    private val pledgeCurrency = contact.map { it?.pledgeCurrency }
    private val pledgeFrequency = contact.map { it?.pledgeFrequency }
    val lastSixDonations = contact.map { it?.getLastSixDonations() }
    val pledgeVisibility = pledgeAmount.combineWith(pledgeCurrency, pledgeFrequency) { amount, currency, frequency ->
        if (amount.isNullOrEmpty() || currency.isNullOrEmpty() || frequency.isNullOrEmpty()) {
            return@combineWith View.GONE
        } else {
            return@combineWith View.VISIBLE
        }
    }
    val numGifts = contact.map {
        stringResolver.getString(R.string.donations_last_gifts, it?.getLastSixDonationIds()?.size)
    }
    val donationLine = contact.map { contact ->
        if (
            !contact?.pledgeAmount.isNullOrEmpty() ||
            !contact?.pledgeCurrency.isNullOrEmpty() ||
            !contact?.pledgeFrequency.isNullOrEmpty()
        ) {
            return@map stringResolver.getString(
                R.string.donation_line,
                currencyFormatter.formatForCurrency(contact?.pledgeAmount, contact?.pledgeCurrency),
                contact?.getPledgeFrequencyString(stringResolver)
            )
        } else {
            return@map ""
        }
    }
    val viewMoreDonationsVisibility = contact.map {
        if (it?.getLastSixDonationIds() != null && it.getLastSixDonationIds()?.size == MAX_GIFTS_VISIBLE) {
            return@map View.VISIBLE
        } else {
            return@map View.INVISIBLE
        }
    }

    // endregion For ContactDetailDonationFragment

    // region Sync logic
    val syncTracker = SyncTracker()

    init {
        distinctContactId.observe(this) { syncData() }
        accountListId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        val contactId = contactId.value ?: return
        val accountListId = accountListId.value

        val tasks = mutableListOf(contactsSyncService.syncContact(contactId, force))
        if (accountListId != null) tasks.add(tasksSyncService.syncTasksForContact(accountListId, contactId, force))
        syncTracker.runSyncTasks(*tasks.toTypedArray())
    }
    // endregion Sync logic
}
