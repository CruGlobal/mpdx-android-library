package org.mpdx.android.features.tasks;

import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.tasks.autolog.AutoLogTaskDialogFragment;
import org.mpdx.android.features.tasks.taskdetail.AllowedActivityTypes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public class TaskModalUtility {
    public static void showDialogIfPending(@NonNull final FragmentActivity activity, AppPrefs appPrefs,
                                           String contactId, @Nullable final String taskId) {
        final String lastId = appPrefs.getLastStartedAppId();
        if ((contactId != null && contactId.equals(lastId)) || (taskId != null && taskId.equals(lastId))) {
            AllowedActivityTypes type = AllowedActivityTypes.forApiValue(appPrefs.getAndClearLastStartedApp());
            if (type != null) {
                new AutoLogTaskDialogFragment(type, contactId, taskId)
                        .show(activity.getSupportFragmentManager().beginTransaction().addToBackStack(null), null);
            }
            appPrefs.setLastStartedAppId(null);
        }
    }
}
