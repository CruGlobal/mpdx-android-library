package org.mpdx.android.features.constants

import androidx.annotation.MainThread
import io.realm.kotlin.where
import java.util.Locale
import org.ccci.gto.android.common.util.LocaleUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.mpdx.android.core.realm.RealmManagerEvent
import org.mpdx.android.features.constants.model.ConstantList
import org.mpdx.android.features.constants.realm.getConstants
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.realm

object Constants {
    const val SOURCE_MPDX = "MPDX"
    const val SOURCE_TNT = "TntImport"

    @Volatile
    @Suppress("ObjectPropertyName")
    private var _constants: ConstantList? = null
    private val constants get() = _constants ?: load()

    val notificationTypes get() = constants.notificationTypes

    // region Currencies
    private inline val currencies get() = constants.currencyOptions
    @JvmStatic
    val currencyLabels get() = currencies.map { it.codeSymbolString }.toTypedArray()
    fun getCurrencyLabel(code: String?): String? = currencies.firstOrNull { it.code == code }?.codeSymbolString
    fun getCurrencyCode(label: String?): String? = currencies.firstOrNull { it.codeSymbolString == label }?.code
    // endregion Currencies

    // region Commitment Frequencies
    val commitmentFrequency get() = constants.commitmentFrequency
    @JvmStatic
    val commitmentFrequencyLabels get() = commitmentFrequency.map { it.name }.toTypedArray()
    fun getCommitmentFrequencyLabel(value: String?): String? =
        constants.commitmentFrequency.firstOrNull { it.id == value }?.name
    fun getCommitmentFrequencyValue(label: String?): String? =
        constants.commitmentFrequency.firstOrNull { it.name == label }?.id
    // endregion Commitment Frequencies

    // region Likeliness To Give
    val likelinessToGive get() = constants.likelinessToGive
    @JvmStatic
    val likelinessToGiveLabels get() = constants.likelinessToGive.map { it.value }.toTypedArray()
    fun getLikelinessToGiveLabel(value: String?): String? =
        constants.likelinessToGive.firstOrNull { it.id == value }?.value
    fun getLikelinessToGiveValue(label: String?): String? =
        constants.likelinessToGive.firstOrNull { it.value == label }?.id
    // endregion Likeliness To Give

    // region Locales
    @JvmStatic
    val localeNames get() = constants.cruLocaleList.map { it.name }.toTypedArray()
    fun getLocaleName(code: String?): String? = constants.cruLocaleList.firstOrNull { it.code == code }?.name
    fun getLocaleCode(name: String?): String? = constants.cruLocaleList.firstOrNull { it.name == name }?.code
    // endregion Locales

    // region Locations
    @JvmStatic
    val locationLabels get() = constants.possibleLocations.map { it.value }.toTypedArray()
    fun getLocationLabel(value: String?): String? = constants.possibleLocations.firstOrNull { it.id == value }?.value
    fun getLocationValue(label: String?): String? = constants.possibleLocations.firstOrNull { it.value == label }?.id
    // endregion Locations

    // region Newsletter Options
    private inline val newsletterOptions get() = constants.newsLetterOptions
    @JvmStatic
    val newsLetterOptionsValues get() = newsletterOptions.map { it.value }.toTypedArray()
    fun getNewsletterOptionLabel(value: String?): String? = newsletterOptions.firstOrNull { it.id == value }?.value
    fun getNewsletterOptionValue(label: String?): String? = newsletterOptions.firstOrNull { it.value == label }?.id
    // endregion Newsletter Options

    // region Statuses
    val statuses get() = constants.statuses
    @JvmStatic
    val statusValues get() = statuses.map { it.value }.toTypedArray()
    fun getStatusLabel(value: String?): String? = statuses.firstOrNull { it.id == value }?.value
    fun getStatusValue(label: String?): String? = statuses.firstOrNull { it.value == label }?.id
    // endregion Statuses

    // region Task Types
    val taskTypes get() = constants.activities
    @JvmStatic
    val taskTypeValues get() = taskTypes.map { it.value }
    fun getTaskTypeLabel(value: String?) = constants.activities.firstOrNull { it.id == value }?.value
    fun getTaskTypeValue(label: String?) = constants.activities.firstOrNull { it.value == label }?.id

    // region Results
    fun getResults(type: String?): List<String> = constants.getResults(type)
    // endregion Results

    // region Next Actions
    @JvmStatic
    fun getNextActionsLabels(type: String?): List<String> =
        getNextActionsValues(type).mapNotNull { getTaskTypeLabel(it) }
    fun getNextActionsValues(type: String?): List<String> = constants.getNextActions(type)
    // endregion Next Actions
    // endregion Task Types

    init {
        EventBus.getDefault().register(this)
    }

    private fun load(): ConstantList {
        return realm {
            val managed = LocaleUtils.getFallbacks(Locale.getDefault(), Locale.ENGLISH).asSequence()
                .mapNotNull { getConstants(it).findFirst() }
                .firstOrNull() ?: where<ConstantList>().findFirst()
            managed?.copyFromRealm() ?: ConstantList()
        }.also { _constants = it }
    }

    @MainThread
    fun reload() {
        _constants = null
        load()
    }

    @MainThread
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRealmManagerEvent(event: RealmManagerEvent) = reload()
}
