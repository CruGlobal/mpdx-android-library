package org.mpdx.android.features.tasks

interface BaseTaskListener {
    fun toTaskDetail(taskId: String?, activityType: String?)
    fun onTaskCompleted(taskId: String?)
}
