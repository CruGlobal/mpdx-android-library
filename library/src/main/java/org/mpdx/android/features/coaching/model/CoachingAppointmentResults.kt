package org.mpdx.android.features.coaching.model

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiPostCreate
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.features.coaching.model.stats.CoachingStatMonthlyGained
import org.mpdx.android.features.coaching.model.stats.CoachingStatMonthlyLost
import org.mpdx.android.features.coaching.model.stats.CoachingStatSpecialGained
import org.mpdx.android.features.coaching.model.stats.CoachingStatWithCurrency
import org.threeten.bp.LocalDate

private const val JSON_API_TYPE_APPOINTMENT_PERIOD_RESULTS = "reports_appointment_results_periods"

@JsonApiType(JSON_API_TYPE_APPOINTMENT_PERIOD_RESULTS)
open class CoachingAppointmentResults : RealmObject(), UniqueItem {
    companion object {
        const val JSON_END_DATE = "end_date"
    }

    // region Attributes

    @Ignore
    @JsonApiAttribute("start_date")
    private var apiStartDate: String? = null
    @JsonApiIgnore
    private var startDate: Long? = null
        set(value) {
            field = value
            generateId()
        }
    private var startLocalDate: LocalDate?
        get() = startDate?.let { LocalDate.ofEpochDay(it) }
        set(value) {
            startDate = value?.toEpochDay()
        }

    @Ignore
    @JsonApiAttribute(JSON_END_DATE)
    private var apiEndDate: String? = null
    @JsonApiIgnore
    private var endDate: Long? = null
        set(value) {
            field = value
            generateId()
        }
    private var endLocalDate: LocalDate?
        get() = endDate?.let { LocalDate.ofEpochDay(it) }
        set(value) {
            endDate = value?.toEpochDay()
        }

    @JsonApiAttribute("group_appointments")
    private var groupAppointments: Int = 0

    @JsonApiAttribute("individual_appointments")
    private var individualAppointments: Int = 0

    @JsonApiAttribute("monthly_decrease")
    private var monthlyDecrease: Float = 0f

    @JsonApiAttribute("monthly_increase")
    private var monthlyIncrease: Float = 0f

    @JsonApiAttribute("new_monthly_partners")
    private var newMonthlyPartners: Int = 0

    @JsonApiAttribute("new_special_pledges")
    private var newSpecialPledges: Float = 0f

    @JsonApiAttribute("pledge_increase")
    private var pledgeIncrease: Double = 0.0

    @JsonApiAttribute("weekly_individual_appointment_goal")
    private var weeklyIndividualAppointmentGoal: Int = 0

    // endregion Attributes

    // region Local Attributes

    @JsonApiIgnore
    var accountListId: String? = null
        set(value) {
            field = value
            generateId()
        }

    // endregion Local Attributes

    // region Generated Attributes

    @JsonApiIgnore
    @PrimaryKey
    override var id: String? = null

    private fun generateId() {
        id = "$accountListId:$startLocalDate:$endLocalDate"
    }

    val stats: Array<CoachingStatWithCurrency<*, *>>
        get() = arrayOf(
            CoachingStatMonthlyGained(Math.round(monthlyIncrease)),
            CoachingStatMonthlyLost(Math.round(monthlyDecrease)),
            CoachingStatSpecialGained(Math.round(pledgeIncrease).toInt())
        )

    // endregion Generated Attributes

    // region API Logic

    @JsonApiPostCreate
    private fun convertStartAndEndDates() {
        startLocalDate = apiStartDate?.let { LocalDate.parse(it) }
        endLocalDate = apiEndDate?.let { LocalDate.parse(it) }
    }

    // endregion API Logic
}
