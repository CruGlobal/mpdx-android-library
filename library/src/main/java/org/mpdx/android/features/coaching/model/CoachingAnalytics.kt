package org.mpdx.android.features.coaching.model

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPostCreate
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.json.JSONObject
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.features.coaching.model.stats.CoachingStat
import org.mpdx.android.features.coaching.model.stats.CoachingStatAppointments
import org.mpdx.android.features.coaching.model.stats.CoachingStatContacts
import org.mpdx.android.features.coaching.model.stats.CoachingStatCorrespondence
import org.mpdx.android.features.coaching.model.stats.CoachingStatElectronic
import org.mpdx.android.features.coaching.model.stats.CoachingStatPhone
import org.threeten.bp.LocalDate

private const val JSON_API_TYPE_COACHING_ANALYTICS = "account_list_analytics"

@JsonApiType(JSON_API_TYPE_COACHING_ANALYTICS)
open class CoachingAnalytics : RealmObject(), UniqueItem {
    @JsonApiIgnore
    @PrimaryKey
    override var id: String? = null
        set(value) {
            field = value
            appointmentsStat?.id = value
            contactsStat?.id = value
            correspondenceStat?.id = value
            electronicStat?.id = value
            phoneStat?.id = value
        }

    // region Attributes

    @Ignore
    private var appointments: JSONObject? = null
    @JsonApiIgnore
    private var appointmentsStat: CoachingStatAppointments? = null

    @Ignore
    private var contacts: JSONObject? = null
    @JsonApiIgnore
    private var contactsStat: CoachingStatContacts? = null

    @Ignore
    private var correspondence: JSONObject? = null
    @JsonApiIgnore
    private var correspondenceStat: CoachingStatCorrespondence? = null

    @Ignore
    private var electronic: JSONObject? = null
    @JsonApiIgnore
    private var electronicStat: CoachingStatElectronic? = null

    @Ignore
    private var phone: JSONObject? = null
    @JsonApiIgnore
    private var phoneStat: CoachingStatPhone? = null

    // endregion Attributes

    // region API Logic

    @JsonApiPostCreate
    private fun translateStats() {
        appointmentsStat = CoachingStatAppointments(id, appointments)
        contactsStat = CoachingStatContacts(id, contacts)
        correspondenceStat = CoachingStatCorrespondence(id, correspondence)
        electronicStat = CoachingStatElectronic(id, electronic)
        phoneStat = CoachingStatPhone(id, phone)

        appointments = null
        contacts = null
        correspondence = null
        electronic = null
        phone = null
    }

    // endregion API Logic

    // region Local Attributes

    @JsonApiIgnore
    var accountListId: String? = null
        set(value) {
            field = value
            generateId()
        }
    @JsonApiIgnore
    private var _startDate: String? = null
        set(value) {
            field = value
            generateId()
        }
    var startDate: LocalDate?
        get() = _startDate?.let { LocalDate.parse(it) }
        set(value) {
            _startDate = value?.toString()
        }
    @JsonApiIgnore
    private var _endDate: String? = null
        set(value) {
            field = value
            generateId()
        }
    var endDate: LocalDate?
        get() = _endDate?.let { LocalDate.parse(it) }
        set(value) {
            _endDate = value?.toString()
        }

    private fun generateId() {
        id = "$accountListId:$_startDate:$_endDate"
    }

    // endregion Local Attributes

    val stats
        get() = arrayOf<CoachingStat<*, *>?>(
            contactsStat,
            appointmentsStat,
            phoneStat,
            electronicStat,
            correspondenceStat
        )
}
