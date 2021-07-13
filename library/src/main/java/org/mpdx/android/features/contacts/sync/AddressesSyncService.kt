package org.mpdx.android.features.contacts.sync

import android.os.Bundle
import dagger.Lazy
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.mpdx.android.base.api.ApiConstants.ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.contacts.api.ContactsApi
import org.mpdx.android.features.contacts.model.Address
import org.mpdx.android.features.contacts.realm.getAddresses
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "AddressesSyncService"

private const val SUBTYPE_ADDRESSES_DIRTY = "addresses_dirty"

private const val ERROR_ADDRESS_NOT_FOUND = "Couldn't find Address with 'id'="
private const val ERROR_UNEDITABLE_SOURCE = "cannot be changed because the source is not MPDX or TntImport"

@Singleton
class AddressesSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val contactsSyncService: Lazy<ContactsSyncService>,
    private val contactsApi: ContactsApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_ADDRESSES_DIRTY -> syncDirtyAddresses(args)
        }
    }

    // region Dirty Address sync
    private val dirtyAddressesMutex = Mutex()
    private suspend fun syncDirtyAddresses(args: Bundle) = dirtyAddressesMutex.withLock {
        coroutineScope {
            realm { getAddresses(true).isDirty().findAll().copyFromRealm() }
                .forEach { address: Address ->
                    launch {
                        if (address.contact?.isNew == true) contactsSyncService.get().syncDirtyContacts().run()
                        when {
                            address.isDeleted -> syncDeletedAddress(address)
                            address.isNew -> syncNewAddress(address)
                            address.hasChangedFields -> syncChangedAddress(address)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewAddress(address: Address) {
        val contactId = address.contact?.id ?: return
        try {
            address.prepareForApi()
            contactsApi.createAddressAsync(contactId, address)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(address)
                        val contact = getContact(contactId).findFirst()
                        saveInRealm(body.data.onEach { it.contact = contact })
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonApiError %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error syncing an address to the API")
        }
    }

    private suspend fun syncChangedAddress(address: Address) {
        val contactId = address.contact?.id ?: return
        val addressId = address.id ?: return
        try {
            contactsApi.updateAddressAsync(contactId, addressId, createPartialUpdate(address))
                .onSuccess { body ->
                    realmTransaction {
                        clearChangedFields(address)
                        val contact = getContact(contactId).findFirst()
                        saveInRealm(body.data.onEach { it.contact = contact })
                    }
                }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // invalid updated_in_db_at time
                            code() == 409 && error.detail?.startsWith(ERROR_UPDATE_IN_DB_AT_INVALID_PREFIX) == true -> {
                                contactsSyncService.get().syncContact(contactId, true).run()
                                return@onError true
                            }

                            // an address field can't be edited, so let's reset it
                            code() == 400 && error.detail?.endsWith(ERROR_UNEDITABLE_SOURCE) == true -> {
                                error.source?.pointer?.substringAfterLast("/", "")?.let {
                                    realmTransaction { getManagedVersion(address)?.clearChanged(it) }
                                }
                                contactsSyncService.get().syncContact(contactId, true).run()
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unhandled JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error storing the changed address %s to the API", addressId)
        }
    }

    private suspend fun syncDeletedAddress(address: Address) {
        val contactId = address.contact?.id ?: return
        val addressId = address.id ?: return
        try {
            contactsApi.deleteAddressAsync(contactId, addressId)
                .onSuccess(requireBody = false) { realmTransaction { deleteObj(address) } }
                .onError { data ->
                    if (code() == 500) return@onError true

                    data?.errors?.forEach { error ->
                        when {
                            // address not found
                            code() == 404 && error.detail?.startsWith(ERROR_ADDRESS_NOT_FOUND) == true -> {
                                // address was already deleted, so let's delete our local copy
                                realmTransaction { deleteObj(address) }
                                return@onError true
                            }
                        }

                        Timber.tag(TAG).e("Unhandled JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error deleting an address")
        }
    }

    fun syncDirtyAddresses() = Bundle().toSyncTask(SUBTYPE_ADDRESSES_DIRTY)
    // endregion Dirty Address sync
}
