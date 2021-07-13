package org.mpdx.android.base.sync

import android.content.ContentResolver.SYNC_EXTRAS_MANUAL
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.kotlin.where
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.JsonApiConverter.Options
import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.ccci.gto.android.common.jsonapi.retrofit2.model.JsonApiRetrofitObject
import org.ccci.gto.android.common.util.os.getLocale
import org.ccci.gto.android.common.util.os.putLocale
import org.mpdx.android.base.api.ApiConstants.JSON_ATTR_UPDATED_IN_DB_AT
import org.mpdx.android.base.api.util.fromJsonOrNull
import org.mpdx.android.base.model.ChangeAwareItem
import org.mpdx.android.base.model.JsonApiModel
import org.mpdx.android.base.model.LocalAttributes
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.base.sync.model.LastSyncTime
import org.mpdx.android.base.sync.model.LastSyncTimeFields
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.getManagedVersionOrNull
import org.mpdx.android.utils.mergeWithExistingModel
import org.mpdx.android.utils.realm
import retrofit2.Response
import timber.log.Timber

private const val ARG_TASK_TYPE = "type"
@VisibleForTesting
internal const val ARG_TASK_SUBTYPE = "subtype"
@VisibleForTesting
internal const val ARG_ACCOUNT_LIST_ID = "account_list_id"
private const val ARG_CONTACT_ID = "contact_id"
private const val ARG_LOCALE = "locale"
private const val ARG_PAGE = "page"

abstract class BaseSyncService(private val syncDispatcher: SyncDispatcher, val jsonApiConverter: JsonApiConverter) {
    private val type = this::class.java.name

    init {
        syncDispatcher.registerSyncService(type, this)
    }

    abstract suspend fun sync(args: Bundle)

    // region Argument Extension Methods
    protected fun Bundle.subType(): String? = getString(ARG_TASK_SUBTYPE)
    protected fun Bundle.putAccountListId(id: String?) = apply { putString(ARG_ACCOUNT_LIST_ID, id) }
    protected fun Bundle.getAccountListId(): String? = getString(ARG_ACCOUNT_LIST_ID)
    protected fun Bundle.putContactId(id: String) = apply { putString(ARG_CONTACT_ID, id) }
    protected fun Bundle.getContactId(): String? = getString(ARG_CONTACT_ID)
    protected fun Bundle.putLocale(locale: Locale?) = apply { putLocale(ARG_LOCALE, locale) }
    protected fun Bundle.getLocale() = getLocale(ARG_LOCALE)
    protected fun Bundle.putPage(page: Int) = apply { putInt(ARG_PAGE, page) }
    protected fun Bundle.getPage() = getInt(ARG_PAGE)
    protected fun Bundle.isForced() = getBoolean(SYNC_EXTRAS_MANUAL, false)
    protected fun Bundle.toSyncTask(subtype: String? = null, force: Boolean = false): SyncTask {
        putString(ARG_TASK_TYPE, type)
        putString(ARG_TASK_SUBTYPE, subtype)
        putBoolean(SYNC_EXTRAS_MANUAL, force)
        return SyncTask(syncDispatcher, this)
    }
    // endregion Argument Extension Methods

    // region Request processing
    protected fun <T> createPartialUpdate(
        model: T,
        optionsBlock: (Options.Builder.() -> Unit)? = null
    ): JsonApiObject<T> where T : JsonApiModel, T : ChangeAwareItem {
        val type = model.jsonApiType
        model.prepareForApi()
        return JsonApiRetrofitObject.single(model).apply {
            options = Options.builder()
                .serializeNullAttributes(type)
                .fields(type, *model.changedFields.toList().toTypedArray())
                .fields(type, JSON_ATTR_UPDATED_IN_DB_AT)
                .apply { optionsBlock?.invoke(this) }
                .build()
        }
    }

    protected suspend fun <T> fetchPages(
        pages: Int = 0,
        fetchPage: suspend (Int) -> Response<JsonApiObject<T>>
    ): List<Response<JsonApiObject<T>>> {
        val channel = Channel<Response<JsonApiObject<T>>>(UNLIMITED)
        fetchPagesInto(channel, pages, fetchPage)
        return channel.toList()
    }

    protected suspend fun <T> fetchPagesInto(
        channel: Channel<Response<JsonApiObject<T>>>,
        pages: Int = 0,
        fetchPage: suspend (Int) -> Response<JsonApiObject<T>>
    ) {
        coroutineScope {
            // fetch the first page
            val page1 = fetchPage(1)
            channel.send(page1)

            // determine how many pages we still need to get
            val totalPages = page1.takeIf { it.isSuccessful }?.body()?.rawMeta
                ?.optJSONObject("pagination")?.optInt("total_pages") ?: 1
            val pagesToFetch = when {
                pages <= 0 -> totalPages
                pages < totalPages -> pages
                else -> totalPages
            }

            // fetch the remaining pages
            for (page in 2..pagesToFetch) { launch { channel.send(fetchPage(page)) } }
        }
        channel.close()
    }
    // endregion Request processing

    // region Response processing

    protected fun <T> Response<JsonApiObject<T>>.hasErrors() = !isSuccessful || body()?.hasErrors() == true
    protected fun <T> List<Response<JsonApiObject<T>>>.hasErrors() = any { it.hasErrors() }

    protected fun <T> Iterable<Response<JsonApiObject<T>>>.aggregateData() = asSequence()
        .filter { it.isSuccessful }
        .mapNotNull { it.body()?.data }
        .flatMap { it.asSequence() }
        .toList()

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal inline fun <T> Response<JsonApiObject<T>>?.onSuccess(
        requireBody: Boolean,
        block: Response<JsonApiObject<T>>.(body: JsonApiObject<T>?) -> Unit
    ): Response<JsonApiObject<T>>? {
        if (this == null) return null

        val body = body()
        if (isSuccessful && body?.hasErrors() != true) {
            if (!requireBody || body != null) {
                block(body)
                return null
            }
        }
        return this
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal inline fun <T> Response<JsonApiObject<T>>?.onSuccess(
        block: Response<JsonApiObject<T>>.(body: JsonApiObject<T>) -> Unit
    ) = onSuccess(requireBody = true) { block(it!!) }

    @Throws(IOException::class)
    protected inline fun <reified T> Response<JsonApiObject<T>>?.onError(
        block: Response<JsonApiObject<T>>.(body: JsonApiObject<T>?) -> Boolean
    ): Response<JsonApiObject<T>>? {
        if (this == null) return null

        val body = body()
        if (!isSuccessful || body == null || body.hasErrors()) {
            val errorBody = errorBody()?.string()
            val handled = block(body ?: errorBody?.let { jsonApiConverter.fromJsonOrNull<T>(it) })
            if (!handled) {
                Timber.tag("BaseSyncService")
                    .e(UnsupportedOperationException(), "Unhandled API error %d response '%s'", code(), errorBody)
            }
            return null
        }
        return this
    }

    // endregion Response processing

    // region Realm Functions

    protected fun getLastSyncTime(vararg rawId: String) = rawId.joinToString("|").let { id ->
        realm {
            where<LastSyncTime>().equalTo(LastSyncTimeFields.ID, id).findFirst()?.copyFromRealm() ?: LastSyncTime(id)
        }
    }

    protected inline fun <T : UniqueItem> RealmQuery<T>.asExisting(): MutableMap<String?, T> = findAll().asExisting()
    protected inline fun <T : UniqueItem> Collection<T>.asExisting() = associateBy { it.id }.toMutableMap()

    protected fun Realm.saveInRealm(item: RealmObject) = saveInRealm(listOf(item))
    protected fun Realm.saveInRealm(
        items: Collection<RealmObject?>,
        existingItems: MutableMap<String?, out RealmObject>? = null,
        deleteOrphanedExistingItems: Boolean = true
    ): Boolean {
        var clean = true

        items.forEach { item ->
            if (item == null) return@forEach

            val id = (item as? UniqueItem)?.id

            // determine if we should store this item based on if the object is dirty in the db
            var storeItem = true
            if (id != null && item is ChangeAwareItem) {
                val existing = existingItems?.get(id)?.takeIf { it.isManaged }
                    ?: where(item::class.java).equalTo(UniqueItem.FIELD_ID, id).findFirst()
                if (existing is ChangeAwareItem) {
                    storeItem = !existing.isDeleted
                }
            }

            // store this item
            if (storeItem) {
                val sanitizedItem = mergeWithExistingModel(item, maxDepth = 4) { model, existing ->
                    when {
                        model is JsonApiModel && model.isPlaceholder &&
                            (!model.replacePlaceholder || existing !is JsonApiModel || !existing.isPlaceholder) ->
                            return@mergeWithExistingModel existing
                        existing is ChangeAwareItem && existing.isDeleted -> return@mergeWithExistingModel existing
                        existing is ChangeAwareItem && existing.hasChangedFields && model is ChangeAwareItem ->
                            model.mergeChangedFields(existing)
                        existing is ChangeAwareItem && existing.hasChangedFields ->
                            return@mergeWithExistingModel existing
                    }

                    if (model is LocalAttributes && existing is LocalAttributes) model.mergeInLocalAttributes(existing)
                    return@mergeWithExistingModel model
                }
                copyToRealmOrUpdate(sanitizedItem)
            } else clean = false

            // remove this item from the existingItems set
            existingItems?.remove(id)
        }

        if (existingItems != null && deleteOrphanedExistingItems) deleteOrphanedItems(existingItems.values)

        return clean
    }

    @Deprecated(
        "Use getManagedVersionOrNull instead",
        ReplaceWith("getManagedVersionOrNull(obj)", "org.mpdx.android.utils.getManagedVersionOrNull")
    )
    protected inline fun <reified T> Realm.getManagedVersion(obj: T) where T : RealmObject, T : UniqueItem =
        getManagedVersionOrNull(obj)

    protected inline fun <reified T> Realm.clearNewFlag(obj: T)
            where T : RealmObject, T : ChangeAwareItem, T : UniqueItem = getManagedVersion(obj)?.apply {
        isNew = false
        clearChangedFieldsMatching(obj)
    }

    protected inline fun <reified T> Realm.clearChangedFields(obj: T)
            where T : RealmObject, T : ChangeAwareItem, T : UniqueItem =
        getManagedVersion(obj)?.clearChangedFieldsMatching(obj)

    protected inline fun <reified T> Realm.deleteObj(obj: T) where T : RealmObject, T : UniqueItem =
        getManagedVersion(obj)?.deleteFromRealm()

    protected fun deleteOrphanedItems(items: Collection<RealmObject>) = items
        // don't remove new items that haven't been synced yet (unless they have been deleted locally)
        .filterNot { it is ChangeAwareItem && it.isNew && !it.isDeleted }
        .forEach { it.deleteFromRealm() }

    // endregion Realm Functions
}

class SyncTask internal constructor(private val syncDispatcher: SyncDispatcher, val args: Bundle) {
    internal val type: String? get() = args.getString(ARG_TASK_TYPE)
    internal val onComplete: MutableList<suspend () -> Unit> = mutableListOf()

    suspend fun run() = syncDispatcher.runSyncTask(this)
    fun launch() = syncDispatcher.launchSyncTask(this)

    fun onComplete(block: suspend () -> Unit) = apply { onComplete.add(block) }
}
