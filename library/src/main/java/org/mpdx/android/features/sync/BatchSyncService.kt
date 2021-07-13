package org.mpdx.android.features.sync

import android.os.Bundle
import androidx.annotation.CheckResult
import dagger.Lazy
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.core.sync.AccountListSyncService
import org.mpdx.android.core.sync.UserSyncService
import org.mpdx.android.features.appeals.sync.AppealsSyncService
import org.mpdx.android.features.appeals.sync.AskedContactsSyncService
import org.mpdx.android.features.appeals.sync.PledgesSyncService
import org.mpdx.android.features.coaching.sync.CoachingSyncService
import org.mpdx.android.features.constants.ConstantsSyncService
import org.mpdx.android.features.contacts.sync.AddressesSyncService
import org.mpdx.android.features.contacts.sync.ContactsSyncService
import org.mpdx.android.features.contacts.sync.DonorAccountSyncService
import org.mpdx.android.features.contacts.sync.EmailAddressSyncService
import org.mpdx.android.features.contacts.sync.FacebookSyncService
import org.mpdx.android.features.contacts.sync.LinkedInSyncService
import org.mpdx.android.features.contacts.sync.PeopleSyncService
import org.mpdx.android.features.contacts.sync.PhoneNumberSyncService
import org.mpdx.android.features.contacts.sync.TwitterSyncService
import org.mpdx.android.features.contacts.sync.WebsiteSyncService
import org.mpdx.android.features.donations.sync.DesignationAccountsSyncService
import org.mpdx.android.features.donations.sync.DonationsSyncService
import org.mpdx.android.features.notifications.sync.NotificationsSyncService
import org.mpdx.android.features.settings.sync.NotificationPreferencesSyncService
import org.mpdx.android.features.tasks.sync.CommentsSyncService
import org.mpdx.android.features.tasks.sync.TasksSyncService

private const val SUBTYPE_BASE = "base"
private const val SUBTYPE_DIRTY = "dirty"

@Singleton
class BatchSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val accountListSyncService: AccountListSyncService,
    private val addressesSyncService: Lazy<AddressesSyncService>,
    private val appealsSyncService: AppealsSyncService,
    private val askedContactsSyncService: Lazy<AskedContactsSyncService>,
    private val coachingSyncService: CoachingSyncService,
    private val commentsSyncService: Lazy<CommentsSyncService>,
    private val constantsSyncService: ConstantsSyncService,
    private val contactsSyncService: ContactsSyncService,
    private val designationAccountsSyncService: DesignationAccountsSyncService,
    private val donationsSyncService: DonationsSyncService,
    private val donorAccountSyncService: DonorAccountSyncService,
    private val emailAddressSyncService: Lazy<EmailAddressSyncService>,
    private val facebookSyncService: Lazy<FacebookSyncService>,
    private val linkedInSyncService: Lazy<LinkedInSyncService>,
    private val notificationsSyncService: Lazy<NotificationsSyncService>,
    private val notificationPreferencesSyncService: Lazy<NotificationPreferencesSyncService>,
    private val peopleSyncService: Lazy<PeopleSyncService>,
    private val phoneNumberSyncService: Lazy<PhoneNumberSyncService>,
    private val pledgesSyncService: Lazy<PledgesSyncService>,
    private val tasksSyncService: TasksSyncService,
    private val twitterSyncService: Lazy<TwitterSyncService>,
    private val userSyncService: UserSyncService,
    private val websiteSyncService: Lazy<WebsiteSyncService>
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_BASE -> syncBaseData(args)
            SUBTYPE_DIRTY -> syncDirtyData(args)
        }
    }

    // region Base Data Sync
    private suspend fun syncBaseData(args: Bundle) = coroutineScope {
        val force = args.isForced()
        launch { constantsSyncService.sync(Locale.getDefault(), force).run() }
        launch { accountListSyncService.syncAccountLists(force).run() }
        launch { coachingSyncService.syncAccountLists(force).run() }
        launch { userSyncService.syncUser(force).run() }

        val accountListId = args.getAccountListId() ?: return@coroutineScope
        launch { appealsSyncService.syncAppeals(accountListId, force).run() }
        launch { contactsSyncService.syncContacts(accountListId).run() }
        launch { contactsSyncService.syncDeletedContacts(force).run() }
        launch { designationAccountsSyncService.syncDesignationAccounts(accountListId).run() }
        launch { donationsSyncService.syncDonationsBatch(accountListId, force = force).run() }
        launch { donorAccountSyncService.syncDonorAccounts(accountListId, force).run() }
        launch { tasksSyncService.syncTasks(accountListId, force).run() }
        launch { tasksSyncService.syncDeletedTasks(force).run() }
    }

    @CheckResult
    @JvmOverloads
    fun syncBaseData(accountListId: String?, force: Boolean = false) = Bundle()
        .putAccountListId(accountListId)
        .toSyncTask(SUBTYPE_BASE, force)
    // endregion Base Data Sync

    // region Dirty Data Sync
    private suspend fun syncDirtyData(args: Bundle) = coroutineScope {
        launch { addressesSyncService.get().syncDirtyAddresses().run() }
        launch { askedContactsSyncService.get().syncDirtyAskedContacts().run() }
        launch { contactsSyncService.syncDirtyContacts().run() }
        launch { donationsSyncService.syncDirtyDonations().run() }
        launch { emailAddressSyncService.get().syncDirtyEmailAddresses().run() }
        launch { notificationsSyncService.get().syncDirtyNotifications().run() }
        launch { notificationPreferencesSyncService.get().syncDirtyNotificationPreferences().run() }
        launch { peopleSyncService.get().syncDirtyPeople().run() }
        launch { phoneNumberSyncService.get().syncDirtyPhoneNumbers().run() }
        launch { pledgesSyncService.get().syncDirtyPledges().run() }
        launch { facebookSyncService.get().syncDirtyFacebookAccounts().run() }
        launch { linkedInSyncService.get().syncDirtyLinkedInAccounts().run() }
        launch { twitterSyncService.get().syncDirtyTwitterAccounts().run() }
        launch { websiteSyncService.get().syncDirtyWebsites().run() }
        launch { tasksSyncService.syncDirtyTasks().run() }
        launch { commentsSyncService.get().syncDirtyComments().run() }
        return@coroutineScope
    }

    @CheckResult
    fun syncDirtyData() = Bundle().toSyncTask(SUBTYPE_DIRTY)
    // endregion Dirty Data Sync
}
