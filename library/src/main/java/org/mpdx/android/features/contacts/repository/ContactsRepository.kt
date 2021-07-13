package org.mpdx.android.features.contacts.repository

import javax.inject.Inject
import javax.inject.Singleton
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.features.contacts.sync.ContactsSyncService
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransactionAsync

@Singleton
class ContactsRepository @Inject constructor(private val contactsSyncService: ContactsSyncService) {
    @Deprecated(message = "This should be handled with a local realm instance")
    fun getSingleCachedContact(id: String?) = realm { getContact(id).findFirst() }

    fun toggleStarred(contactId: String?) {
        realmTransactionAsync(onSuccess = contactsSyncService.syncDirtyContacts()::launch) {
            getContact(contactId).findFirst()?.apply {
                trackingChanges = true
                isStarred = !isStarred
            }
        }
    }
}
