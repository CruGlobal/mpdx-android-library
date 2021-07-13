package org.mpdx.android.features.coaching.model.stats

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.json.JSONObject
import org.mpdx.android.R
import org.mpdx.android.base.model.UniqueItem

open class CoachingStatContacts(
    @PrimaryKey
    override var id: String? = null,
    json: JSONObject? = null
) : RealmObject(), UniqueItem, CoachingStat<Int, Int> {
    private var active = json?.optInt("active", 0) ?: 0
    private var referrals = json?.optInt("referrals", 0) ?: 0
    private var referralsOnHand = json?.optInt("referrals_on_hand", 0) ?: 0

    override val label get() = R.string.stat_contacts
    override val drawable get() = R.drawable.cru_icon_contacts_stat
    override val map
        get() = linkedMapOf(
            R.string.coaching_stat_active to active,
            R.string.coaching_stat_referrals to referrals,
            R.string.coaching_stat_referrals_on_hand to referralsOnHand
        )
}
