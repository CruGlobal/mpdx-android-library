package org.mpdx.android.features.tasks.editor

import android.widget.TextView
import org.mpdx.android.features.tasks.viewmodel.TaskViewModel

interface TaskEditorCallbacks {
    fun showDueDateDatePicker()
    fun showDueDateTimePicker()
    fun showCompletedAtDatePicker()
    fun showCompletedAtTimePicker()
    fun showAddContactSelector()
    fun showUserSelector()

    fun TaskViewModel?.onAddTag(view: TextView?): Boolean {
        if (this == null) return false
        view?.text?.toString()?.let { addTag(it); view.text = null }
        return true
    }

    fun TaskViewModel?.onRemoveTag(view: TextView?) {
        if (this == null) return
        view?.text?.toString()?.let { deleteTag(it) }
    }
}
