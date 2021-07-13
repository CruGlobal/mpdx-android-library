package org.mpdx.android.features.tasks.tasklist

import org.mpdx.android.features.tasks.BaseTaskListener
import org.mpdx.android.features.tasks.model.Task

interface TasksViewAllListener : BaseTaskListener {
    fun toViewAll(grouping: TaskDueDateGrouping?)
    fun onTaskDeleted(response: Task?)
}
