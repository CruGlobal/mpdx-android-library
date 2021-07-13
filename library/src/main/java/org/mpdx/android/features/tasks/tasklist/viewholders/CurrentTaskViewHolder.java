package org.mpdx.android.features.tasks.tasklist.viewholders;

import org.mpdx.android.databinding.ItemTaskBinding;
import org.mpdx.android.features.tasks.model.Task;
import org.mpdx.android.features.tasks.tasklist.CurrentTasksListener;
import org.mpdx.android.features.tasks.viewmodel.TaskViewModel;
import org.mpdx.android.utils.StringResolver;

import androidx.recyclerview.widget.RecyclerView;

public class CurrentTaskViewHolder extends RecyclerView.ViewHolder {
    private ItemTaskBinding binding;

    public CurrentTaskViewHolder(ItemTaskBinding binding, StringResolver stringResolver, CurrentTasksListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.binding.setTask(new TaskViewModel());
        this.binding.setListener(listener);
        this.binding.setResolver(stringResolver);
    }

    public void update(Task task) {
        binding.getTask().setModel(task);
        binding.executePendingBindings();
    }
}
