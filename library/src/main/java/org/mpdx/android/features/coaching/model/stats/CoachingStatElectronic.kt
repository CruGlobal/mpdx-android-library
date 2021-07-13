package org.mpdx.android.features.coaching.model.stats

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.json.JSONObject
import org.mpdx.android.R
import org.mpdx.android.base.model.UniqueItem

open class CoachingStatElectronic(
    @PrimaryKey
    override var id: String? = null,
    json: JSONObject? = null
) : RealmObject(), UniqueItem, CoachingStat<Int, Int> {
    private var appointments = json?.optInt("appointments", 0) ?: 0
    private var received = json?.optInt("received", 0) ?: 0
    private var sent = json?.optInt("sent", 0) ?: 0

    override val label get() = R.string.stat_messaging
    override val drawable get() = R.drawable.cru_icon_message

    override val map
        get() = linkedMapOf(
            R.string.coaching_stat_sent to sent,
            R.string.coaching_stat_received to received,
            R.string.coaching_stat_appointments to appointments
        )
}
