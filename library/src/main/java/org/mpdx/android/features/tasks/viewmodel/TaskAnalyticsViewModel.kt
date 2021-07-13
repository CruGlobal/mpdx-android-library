package org.mpdx.android.features.tasks.viewmodel

import androidx.databinding.Bindable
import org.mpdx.android.base.lifecycle.RealmModelViewModel
import org.mpdx.android.features.tasks.model.COMPARATOR_OVERDUE_TASKS
import org.mpdx.android.features.tasks.model.TaskAnalytics

class TaskAnalyticsViewModel : RealmModelViewModel<TaskAnalytics>() {
    @get:Bindable
    val sortedOverdueTasks get() = model?.overdueTasks?.sortedWith(COMPARATOR_OVERDUE_TASKS)
    @get:Bindable
    val totalTasksDue get() = model?.totalTasksDueCount ?: 0
}
