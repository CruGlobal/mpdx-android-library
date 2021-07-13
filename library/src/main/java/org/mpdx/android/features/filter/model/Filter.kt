package org.mpdx.android.features.filter.model

import androidx.annotation.StringRes
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.mpdx.android.R
import org.mpdx.android.base.model.UniqueItem

open class Filter() : RealmObject(), UniqueItem {
    companion object {
        const val CONTAINER_TASK = 0
        const val CONTAINER_CONTACT = 1
        const val CONTAINER_NOTIFICATION = 2
    }

    constructor(container: Int, type: Type, key: String?, translatedLabel: String?) : this() {
        this.container = container
        this.type = type
        this.key = key
        this.translatedLabel = translatedLabel
    }

    @PrimaryKey
    override var id: String? = null

    var container: Int? = null
        private set(value) {
            field = value
            generateId()
        }
    private var _type: Int? = null
        set(value) {
            field = value
            generateId()
        }
    var type
        get() = _type?.let { Type.values()[it] }
        private set(value) {
            _type = value?.ordinal
        }
    var key: String? = null
        private set(value) {
            field = value
            generateId()
            generateLabel()
        }

    var translatedLabel: String? = null
        set(value) {
            field = value
            generateLabel()
        }
    var label: String? = null
        private set

    var isEnabled: Boolean = false

    private fun generateId() {
        id = "$container.$_type.$key"
    }

    private fun generateLabel() {
        label = translatedLabel?.takeIf { it.isNotEmpty() } ?: key
    }

    enum class Type(@JvmField @StringRes val label: Int) : UniqueItem {
        ACTION_TYPE(R.string.filter_action_type),
        NOTIFICATION_TYPES(R.string.filter_notification_types),
        CONTACT_STATUS(R.string.filter_contact_status),
        CONTACT_REFERRER(R.string.filter_contact_referrer),
        CONTACT_CHURCH(R.string.filter_contact_church),
        CONTACT_LIKELY_TO_GIVE(R.string.filter_contact_likely_to_give),
        CONTACT_TIMEZONE(R.string.filter_contact_timezone),
        CONTACT_CITY(R.string.filter_task_contact_city),
        CONTACT_STATE(R.string.filter_task_contact_state),
        CONTACT_TAGS(R.string.filter_task_tags),
        TASK_TAGS(R.string.filter_task_tags);

        override val id get() = ordinal.toString()
    }
}
