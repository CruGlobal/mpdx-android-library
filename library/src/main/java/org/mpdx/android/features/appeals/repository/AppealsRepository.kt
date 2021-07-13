package org.mpdx.android.features.appeals.repository

import javax.inject.Inject
import javax.inject.Singleton
import org.mpdx.android.features.appeals.realm.createAskedContact
import org.mpdx.android.features.appeals.realm.getAppeal
import org.mpdx.android.features.appeals.sync.AskedContactsSyncService
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.utils.realmTransactionAsync

@Singleton
class AppealsRepository @Inject constructor(private val askedContactsSyncService: AskedContactsSyncService) {
    fun addAskedContact(appealId: String?, contactId: String?, forceDeletion: Boolean = false) {
        realmTransactionAsync(askedContactsSyncService.syncDirtyAskedContacts()::launch) {
            val appeal = getAppeal(appealId).findFirst() ?: return@realmTransactionAsync
            val contact = getContact(contactId).findFirst() ?: return@realmTransactionAsync
            copyToRealm(createAskedContact(appeal, contact)).apply {
                forceListDeletion = forceDeletion
            }
        }
    }
}
