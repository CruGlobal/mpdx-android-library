package org.mpdx.android.features.coaching.model.stats

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.json.JSONObject
import org.mpdx.android.R
import org.mpdx.android.base.model.UniqueItem

open class CoachingStatCorrespondence(
    @PrimaryKey
    override var id: String? = null,
    json: JSONObject? = null
) : RealmObject(), UniqueItem, CoachingStat<Int, Int> {
    private var precall = json?.optInt("precall", 0) ?: 0
    private var reminders = json?.optInt("reminders", 0) ?: 0
    private var supportLetters = json?.optInt("support_letters", 0) ?: 0
    private var thankYous = json?.optInt("thank_yous", 0) ?: 0

    override val label get() = R.string.coaching_stat_header_correspondence
    override val drawable get() = R.drawable.cru_icon_pencil
    override val map
        get() = linkedMapOf(
            R.string.coaching_stat_precall to precall,
            R.string.coaching_stat_support_letters to supportLetters,
            R.string.coaching_stat_thank_yous to thankYous,
            R.string.coaching_stat_reminders to reminders
        )
}
