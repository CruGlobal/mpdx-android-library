package org.mpdx.android.features.tasks.tasklist.viewholders;

import org.mpdx.android.CurrentTaskHeaderItemBinding;
import org.mpdx.android.features.tasks.tasklist.CurrentTasksListener;
import org.mpdx.android.features.tasks.tasklist.TaskDueDateGrouping;
import org.mpdx.android.utils.StringResolver;

import androidx.recyclerview.widget.RecyclerView;

public class CurrentTaskHeaderViewHolder extends RecyclerView.ViewHolder {
    private CurrentTaskHeaderItemBinding binding;

    public CurrentTaskHeaderViewHolder(CurrentTaskHeaderItemBinding binding, StringResolver stringResolver, CurrentTasksListener listener) {
        super(binding.getRoot());
        this.binding = binding;
       this.binding.setListener(listener);
    }

    public void update(TaskDueDateGrouping grouping) {
        binding.setGrouping(grouping);
        binding.executePendingBindings();
    }
}
