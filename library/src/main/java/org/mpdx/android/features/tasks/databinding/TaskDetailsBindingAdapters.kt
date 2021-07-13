package org.mpdx.android.features.tasks.databinding

import android.view.View
import androidx.databinding.BindingAdapter
import org.mpdx.android.features.tasks.model.Task

@BindingAdapter("visibleIfTaskHasResult")
fun visibleIfTaskHasResult(view: View, task: Task?) {
    view.visibility = if (task?.isCompleted == true && task.result != null) View.VISIBLE else View.GONE
}
