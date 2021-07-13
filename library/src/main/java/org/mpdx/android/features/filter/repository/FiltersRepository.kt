package org.mpdx.android.features.filter.repository

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import io.realm.Realm
import javax.inject.Inject
import javax.inject.Singleton
import org.mpdx.android.R
import org.mpdx.android.base.model.TagsConcern
import org.mpdx.android.features.constants.Constants
import org.mpdx.android.features.contacts.model.Address
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.realm.getContacts
import org.mpdx.android.features.filter.FILTER_CONTACT_STATUS_NONE
import org.mpdx.android.features.filter.HIDDEN_STATUSES
import org.mpdx.android.features.filter.model.Filter
import org.mpdx.android.features.filter.realm.forContainer
import org.mpdx.android.features.filter.realm.getFilters
import org.mpdx.android.features.filter.realm.key
import org.mpdx.android.features.filter.realm.type
import org.mpdx.android.features.tasks.realm.getTasks
import org.mpdx.android.utils.realmTransactionAsync

@Singleton
class FiltersRepository @Inject constructor() {
    fun toggleFilter(container: Int?, type: Filter.Type?, key: String?, isEnabled: Boolean) {
        realmTransactionAsync {
            getFilters().forContainer(container).type(type).key(key).findAll().forEach { it.isEnabled = isEnabled }
        }
    }

    fun toggleAllFilters(container: Int?, type: Filter.Type?, isEnabled: Boolean) {
        realmTransactionAsync {
            getFilters().forContainer(container).type(type).findAll().forEach { it.isEnabled = isEnabled }
        }
    }

    fun toggleContactStatusFilters(container: Int?, showActive: Boolean, showHidden: Boolean = !showActive) {
        realmTransactionAsync {
            getFilters().forContainer(container).type(Filter.Type.CONTACT_STATUS).findAll().forEach {
                it.isEnabled = if (HIDDEN_STATUSES.contains(it.key)) showHidden else showActive
            }
        }
    }

    @AnyThread
    fun updateContactFilters(context: Context) {
        realmTransactionAsync {
            val contacts = getContacts().findAll()
            val addresses = contacts.getAddresses()

            val filters = buildContactStatusFilters(Filter.CONTAINER_CONTACT, context) +
                contacts.buildChurchFilters(Filter.CONTAINER_CONTACT) +
                contacts.buildLikelyToGiveFilters(Filter.CONTAINER_CONTACT) +
                contacts.buildTimezoneFilters(Filter.CONTAINER_CONTACT) +
                contacts.buildReferrerFilters(Filter.CONTAINER_CONTACT) +
                contacts.buildTagFilters(Filter.CONTAINER_CONTACT, Filter.Type.CONTACT_TAGS) +
                addresses.buildCityFilters(Filter.CONTAINER_CONTACT) +
                addresses.buildStateFilters(Filter.CONTAINER_CONTACT)

            updateFilters(Filter.CONTAINER_CONTACT, filters.filterNot { it.key.isNullOrEmpty() })
        }
    }

    @AnyThread
    fun updateTaskFilters(accountListId: String, context: Context) {
        realmTransactionAsync {
            val tasks = getTasks(accountListId).findAll()
            val contacts = tasks.asSequence()
                .flatMap { it.getContacts()?.findAll()?.asSequence().orEmpty() }.distinctBy { it.id }.toList()
            val addresses = contacts.getAddresses()

            val filters = buildTaskTypeFilters() +
                buildContactStatusFilters(Filter.CONTAINER_TASK, context) +
                contacts.buildChurchFilters(Filter.CONTAINER_TASK) +
                contacts.buildLikelyToGiveFilters(Filter.CONTAINER_TASK) +
                contacts.buildTimezoneFilters(Filter.CONTAINER_TASK) +
                contacts.buildReferrerFilters(Filter.CONTAINER_TASK) +
                addresses.buildCityFilters(Filter.CONTAINER_TASK) +
                addresses.buildStateFilters(Filter.CONTAINER_TASK) +
                tasks.buildTagFilters(Filter.CONTAINER_TASK, Filter.Type.TASK_TAGS)

            updateFilters(Filter.CONTAINER_TASK, filters.filterNot { it.key.isNullOrEmpty() })
        }
    }

    @AnyThread
    fun updateNotificationFilters() {
        realmTransactionAsync {
            val filters = Constants.notificationTypes.asSequence()
                .map { Filter(Filter.CONTAINER_NOTIFICATION, Filter.Type.NOTIFICATION_TYPES, it.key, it.value) }

            updateFilters(Filter.CONTAINER_NOTIFICATION, filters)
        }
    }

    @WorkerThread
    private fun Realm.updateFilters(container: Int, filters: Sequence<Filter>) {
        val existing = getFilters().forContainer(container).findAll()
            .map { it.id to it }.toMap(mutableMapOf<String?, Filter>())

        // either update the label for existing filters or insert the new filter
        filters.forEach {
            existing.remove(it.id)?.apply { translatedLabel = it.translatedLabel }
                ?: insert(it)
        }

        // remove any orphaned filters
        existing.values.forEach { it.deleteFromRealm() }
    }

    private fun List<Contact>.getAddresses(): List<Address> {
        return asSequence().flatMap { it.getAddresses()?.findAll()?.asSequence().orEmpty() }.distinctBy { it.id }
            .toList()
    }

    private fun buildContactStatusFilters(container: Int, context: Context): Sequence<Filter> {
        return Constants.statuses.asSequence().map { Filter(container, Filter.Type.CONTACT_STATUS, it.id, it.value) } +
            Filter(container, Filter.Type.CONTACT_STATUS, FILTER_CONTACT_STATUS_NONE, context.getString(R.string.none))
    }

    private fun buildTaskTypeFilters(): Sequence<Filter> {
        return Constants.taskTypes.asSequence()
            .map { Filter(Filter.CONTAINER_TASK, Filter.Type.ACTION_TYPE, it.id, it.value) }
    }

    private fun List<Contact>.buildChurchFilters(container: Int): Sequence<Filter> {
        return asSequence().mapNotNull { it.churchName }.distinct()
            .map { Filter(container, Filter.Type.CONTACT_CHURCH, it, it) }
    }

    private fun List<Contact>.buildLikelyToGiveFilters(container: Int): Sequence<Filter> {
        return asSequence().mapNotNull { it.likelyToGive }.distinct()
            .map { Filter(container, Filter.Type.CONTACT_LIKELY_TO_GIVE, it, Constants.getLikelinessToGiveLabel(it)) }
    }

    private fun List<Contact>.buildTimezoneFilters(container: Int): Sequence<Filter> {
        return asSequence().mapNotNull { it.timezone }.distinct()
            .map { Filter(container, Filter.Type.CONTACT_TIMEZONE, it, it) }
    }

    private fun List<Contact>.buildReferrerFilters(container: Int): Sequence<Filter> {
        return asSequence()
            .flatMap { it.referredBy?.asSequence().orEmpty() }.distinctBy { it.id }
            .filter { it.id != null && it.name != null }
            .map { Filter(container, Filter.Type.CONTACT_REFERRER, it.id, it.name) }
    }

    private fun List<Address>.buildCityFilters(container: Int): Sequence<Filter> {
        return asSequence().mapNotNull { it.city }.distinct()
            .map { Filter(container, Filter.Type.CONTACT_CITY, it, it) }
    }

    private fun List<Address>.buildStateFilters(container: Int): Sequence<Filter> {
        return asSequence().mapNotNull { it.state }.distinct()
            .map { Filter(container, Filter.Type.CONTACT_STATE, it, it) }
    }

    private fun List<TagsConcern>.buildTagFilters(container: Int, type: Filter.Type): Sequence<Filter> {
        return asSequence().flatMap { it.tags?.asSequence().orEmpty() }.distinct()
            .map { Filter(container, type, it, it) }
    }
}
