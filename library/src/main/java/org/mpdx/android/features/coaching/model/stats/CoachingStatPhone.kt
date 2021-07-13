package org.mpdx.android.features.coaching.model.stats

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.json.JSONObject
import org.mpdx.android.R
import org.mpdx.android.base.model.UniqueItem

open class CoachingStatPhone(
    @PrimaryKey
    override var id: String? = null,
    json: JSONObject? = null
) : RealmObject(), UniqueItem, CoachingStat<Int, Int> {
    private var appointments = json?.optInt("appointments", 0) ?: 0
    private var attempted = json?.optInt("attempted", 0) ?: 0
    private var completed = json?.optInt("completed", 0) ?: 0
    private var received = json?.optInt("received", 0) ?: 0
    private var talktoinperson = json?.optInt("talktoinperson", 0) ?: 0

    override val label get() = R.string.stat_phone_calls
    override val drawable get() = R.drawable.cru_icon_phone
    override val map
        get() = linkedMapOf(
            R.string.coaching_stat_attempted to attempted,
            R.string.coaching_stat_talktoinperson to talktoinperson,
            R.string.coaching_stat_appointments to appointments
        )
}
