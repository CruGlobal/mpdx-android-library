package org.mpdx.android.features.constants.model

import androidx.annotation.VisibleForTesting
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.Locale
import org.ccci.gto.android.common.compat.util.LocaleCompat
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPostCreate
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.core.data.api.models.CRUCurrency
import org.mpdx.android.core.data.api.models.CommitmentFrequency
import org.mpdx.android.core.data.models.CRULocale
import org.mpdx.android.core.data.typeadapter.CRUCurrencyMap
import org.mpdx.android.core.data.typeadapter.CRUListMap
import org.mpdx.android.core.data.typeadapter.CRULocaleList
import org.mpdx.android.core.data.typeadapter.StringMapWrapper
import org.mpdx.android.utils.toRealmList

@JsonApiType("constant_list")
open class ConstantList : RealmObject(), UniqueItem {
    companion object {
        const val STATUS_PARTNER_FINANCIAL = "Partner - Financial"
    }

    @PrimaryKey
    @JsonApiIgnore
    override var id: String? = null

    // region Attributes
    @JsonApiAttribute("activity_hashes")
    var activities: RealmList<CruHashes> = RealmList()
        @VisibleForTesting
        internal set

    @JsonApiAttribute("assignable_likely_to_give_hashes")
    var likelinessToGive: RealmList<CruHashes> = RealmList()
        private set

    @JsonApiAttribute("assignable_send_newsletter_hashes")
    var newsLetterOptions: RealmList<CruHashes> = RealmList()
        private set

    @JsonApiAttribute("notification_hashes")
    var notificationTypes: RealmList<CruHashes> = RealmList()
        private set

    @JsonApiAttribute("assignable_location_hashes")
    var possibleLocations: RealmList<CruHashes> = RealmList()
        private set

    @JsonApiAttribute("status_hashes")
    var statuses: RealmList<CruHashes> = RealmList()
        private set

    @Ignore
    @JsonApiAttribute("pledge_frequencies")
    private var ignoreCommitmentFrequency: StringMapWrapper? = null

    @Ignore
    @JsonApiAttribute("results")
    private var ignoreResults: CRUListMap<String>? = null

    @Ignore
    @JsonApiAttribute("next_actions")
    private var ignoreNextActions: CRUListMap<String>? = null

    @Ignore
    @JsonApiAttribute("pledge_currencies")
    private var ignoreCurrency: CRUCurrencyMap? = null

    @Ignore
    @JsonApiAttribute("locales")
    private var ignoreCruLocaleList: CRULocaleList? = null

    private var resultMapping: RealmList<CruHashes>? = null
    @VisibleForTesting
    internal var nextActionMapping: RealmList<CruHashes>? = null
    var commitmentFrequency: RealmList<CommitmentFrequency> = RealmList()
        private set
    var cruLocaleList: RealmList<CRULocale> = RealmList()
        private set
    @JsonApiIgnore
    var currencyOptions: RealmList<CRUCurrency> = RealmList()
        private set
    // endregion Attributes

    // region Generated Attributes
    var locale: Locale?
        get() = id?.let { LocaleCompat.forLanguageTag(it) }
        set(value) {
            id = value?.let { LocaleCompat.toLanguageTag(value) }
        }
    // endregion Generated Attributes

    fun getNextActions(type: String?): List<String> {
        val actions = nextActionMapping?.firstOrNull { it.id.equals(type, true) }
            ?: nextActionMapping?.firstOrNull { it.id.equals("default", true) }

        return actions?.value?.split(",").orEmpty()
    }

    fun getResults(type: String?): List<String> {
        val results = resultMapping?.firstOrNull { it.id.equals(type, true) }
            ?: resultMapping?.firstOrNull { it.id.equals("default", true) }

        return results?.value?.split(",").orEmpty()
    }

    // region API methods
    @JsonApiPostCreate
    private fun sortLikelyToGive() = likelinessToGive.sortBy { it.value }
    @JsonApiPostCreate
    private fun sortNewsletterOptions() = newsLetterOptions.sortBy { it.value }
    @JsonApiPostCreate
    private fun sortPossibleLocations() = possibleLocations.sortBy { it.value }
    @JsonApiPostCreate
    private fun sortTaskTypes() = activities.sortBy { it.value }

    @JsonApiPostCreate
    private fun cleanup() {
        cruLocaleList = ignoreCruLocaleList?.sortedBy { it.name }.orEmpty().toRealmList()
        currencyOptions = ignoreCurrency?.values?.sortedBy { it.codeSymbolString }.orEmpty().toRealmList()

        commitmentFrequency = ignoreCommitmentFrequency?.map?.map {
            CommitmentFrequency().apply {
                id = it.key
                name = it.value
            }
        }?.sortedBy { it.name }.orEmpty().toRealmList()

        resultMapping = ignoreResults?.map {
            CruHashes().apply {
                id = it.key
                value = it.value.joinToString(",")
            }
        }.orEmpty().toRealmList()

        nextActionMapping = ignoreNextActions?.map {
            CruHashes().apply {
                id = it.key
                value = it.value.joinToString(",")
            }
        }.orEmpty().toRealmList()
    }
    // endregion API methods
}
