package org.mpdx.android.features.coaching.model.stats

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.json.JSONObject
import org.mpdx.android.R
import org.mpdx.android.base.model.UniqueItem

open class CoachingStatAppointments(
    @PrimaryKey
    override var id: String? = null,
    json: JSONObject? = null
) : RealmObject(), UniqueItem, CoachingStat<Int, Int> {
    private var completed = json?.optInt("completed", 0) ?: 0
    private var scheduled = json?.optInt("scheduled", 0) ?: 0

    override val label get() = R.string.stat_appointments
    override val drawable get() = R.drawable.cru_icon_calendar
    override val map
        get() = linkedMapOf(
            R.string.coaching_stat_completed to completed,
            R.string.coaching_stat_scheduled to scheduled
        )
}
