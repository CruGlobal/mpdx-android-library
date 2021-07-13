package org.mpdx.android.features.tasks.taskdetail;

import androidx.annotation.Nullable;

public interface TaskDetailListener {
    void onTaskComplete(@Nullable String taskId);

    void onTaskDelete(@Nullable String taskId);

    void onActionClicked();
}
