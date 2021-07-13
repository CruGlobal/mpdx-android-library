package org.mpdx.android.features.contacts.contactdetail.tasks

import org.mpdx.android.features.tasks.BaseTaskListener
import org.mpdx.android.features.tasks.model.Task

interface ContactTasksViewListener : BaseTaskListener {
    fun onTaskDeleted(response: Task?)
    fun onTaskHistoryClicked()
}
