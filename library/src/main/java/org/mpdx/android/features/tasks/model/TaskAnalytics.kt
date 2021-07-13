package org.mpdx.android.features.tasks.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.Date
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType
import org.mpdx.android.base.model.UniqueItem

private const val JSON_API_TYPE_TASK_ANALYTICS = "task_analytics"

@JsonApiType(JSON_API_TYPE_TASK_ANALYTICS)
open class TaskAnalytics : RealmObject(), UniqueItem {
    @PrimaryKey
    @JsonApiIgnore
    override var id: String? = null

    // region Attributes

    @JsonApiAttribute("created_at")
    private var createdAt: Date? = null

    @JsonApiAttribute("last_electronic_newsletter_completed_at")
    var lastElectronicNewsletterCompletedAt: Date? = null
        private set

    @JsonApiAttribute("last_physical_newsletter_completed_at")
    var lastPhysicalNewsletterCompletedAt: Date? = null
        private set

    @JsonApiAttribute("tasks_overdue_or_due_today_counts")
    var overdueTasks: RealmList<OverdueTask>? = null
        private set

    @JsonApiAttribute("total_tasks_due_count")
    var totalTasksDueCount: Int = 0
        private set

    @JsonApiAttribute("updated_at")
    private var updatedAt: Date? = null

    @JsonApiAttribute("updated_in_db_at")
    private var updatedInDbAt: Date? = null

    // endregion Attributes

    // region Local Attributes

    @JsonApiIgnore
    var accountListId: String? = null
        set(value) {
            field = value
            generateId()
        }

    private fun generateId() {
        id = accountListId
    }

    // endregion Local Attributes
}
