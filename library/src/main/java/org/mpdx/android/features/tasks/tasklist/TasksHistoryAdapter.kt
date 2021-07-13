package org.mpdx.android.features.tasks.tasklist

import android.view.LayoutInflater
import android.view.ViewGroup
import org.mpdx.android.base.widget.recyclerview.UniqueItemDataBindingAdapter
import org.mpdx.android.databinding.ItemTaskBinding
import org.mpdx.android.features.tasks.BaseTaskListener
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.viewmodel.TaskViewModel
import org.mpdx.android.utils.StringResolver

internal class TasksHistoryAdapter(
    private val baseListener: BaseTaskListener,
    private val stringResolver: StringResolver
) : UniqueItemDataBindingAdapter<Task, ItemTaskBinding>() {
    // region Lifecycle
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            this.task = TaskViewModel()
            this.resolver = stringResolver
            this.listener = baseListener
        }

    override fun onBindViewDataBinding(binding: ItemTaskBinding, position: Int) {
        binding.task?.model = getItem(position)
    }
    // endregion Lifecycle
}
