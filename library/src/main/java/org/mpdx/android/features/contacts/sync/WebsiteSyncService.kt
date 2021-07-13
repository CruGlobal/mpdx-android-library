package org.mpdx.android.features.contacts.sync

import android.os.Bundle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.base.sync.BaseSyncService
import org.mpdx.android.base.sync.SyncDispatcher
import org.mpdx.android.features.contacts.api.SocialMediaApi
import org.mpdx.android.features.contacts.model.Website
import org.mpdx.android.features.contacts.realm.getPerson
import org.mpdx.android.features.contacts.realm.getWebsites
import org.mpdx.android.features.tasks.sync.SUBTYPE_TASKS_DIRTY
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransaction
import timber.log.Timber

private const val TAG = "WebsiteSyncService"

private const val SUBTYPE_WEBSITES_DIRTY = "websites_dirty"

@Singleton
class WebsiteSyncService @Inject constructor(
    syncDispatcher: SyncDispatcher,
    jsonApiConverter: JsonApiConverter,
    private val peopleSyncService: PeopleSyncService,
    private val mediaApi: SocialMediaApi
) : BaseSyncService(syncDispatcher, jsonApiConverter) {
    override suspend fun sync(args: Bundle) {
        when (args.subType()) {
            SUBTYPE_TASKS_DIRTY -> syncDirtyWebsites(args)
        }
    }

    // region Dirty Website Account sync

    private val dirtyWebsitesMutex = Mutex()
    private suspend fun syncDirtyWebsites(args: Bundle) = dirtyWebsitesMutex.withLock {
        coroutineScope {
            realm { copyFromRealm(getWebsites().isDirty().findAll()) }
                .forEach { website: Website ->
                    launch {
                        if (website.person?.isNew == true) peopleSyncService.syncDirtyPeople().run()
                        when {
                            website.isDeleted -> syncDeletedWebsite(website)
                            website.isNew -> syncNewWebsite(website)
                            website.hasChangedFields -> syncChangedWebsite(website)
                        }
                    }
                }
        }
    }

    private suspend fun syncNewWebsite(website: Website) {
        val contactId = website.person?.contact?.id ?: return
        val personId = website.person?.id ?: return
        try {
            website.prepareForApi()
            mediaApi.createWebsite(contactId, personId, website)
                .onSuccess { body ->
                    realmTransaction {
                        clearNewFlag(website)
                        val person = getPerson(personId).findFirst()
                        saveInRealm(body.data.onEach { it.person = person })
                    }
                }
                .onError { data ->
                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unhandled JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error syncing a new website Account to the API")
        }
    }

    private suspend fun syncChangedWebsite(website: Website) {
        val contactId = website.person?.contact?.id ?: return
        val personId = website.person?.id ?: return
        val websiteId = website.id ?: return
        try {
            mediaApi.updateWebsite(contactId, personId, websiteId, createPartialUpdate(website))
                .onSuccess { body ->
                    realmTransaction {
                        clearChangedFields(website)
                        val person = getPerson(personId).findFirst()
                        saveInRealm(body.data.onEach { it.person = person })
                    }
                }
                .onError { data ->
                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG)
                .d(e, "Error storing the changed email address %s to the API", websiteId)
        }
    }

    private suspend fun syncDeletedWebsite(website: Website) {
        val contactId = website.person?.contact?.id ?: return
        val personId = website.person?.id ?: return
        val websiteId = website.id ?: return
        try {
            mediaApi.deleteWebsite(contactId, personId, websiteId)
                .onSuccess(requireBody = false) { realmTransaction { deleteObj(website) } }
                .onError { data ->
                    data?.errors?.forEach { error ->
                        Timber.tag(TAG).e("Unrecognized JsonApiError: %s", error.detail)
                    }
                    return@onError false
                }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Error deleting an website Account")
        }
    }

    fun syncDirtyWebsites() = Bundle().toSyncTask(SUBTYPE_WEBSITES_DIRTY)

    // endregion Dirty Website Account sync
}
