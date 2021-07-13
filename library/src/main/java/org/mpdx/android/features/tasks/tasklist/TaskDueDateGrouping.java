package org.mpdx.android.features.tasks.tasklist;

import org.mpdx.android.R;
import org.threeten.bp.LocalDate;

import androidx.annotation.StringRes;

public enum TaskDueDateGrouping {
    OVERDUE(R.string.overdue),
    TODAY(R.string.today),
    OVERDUE_OR_TODAY(R.string.today),
    TOMORROW(R.string.tomorrow),
    UPCOMING(R.string.upcoming),
    NO_DUE_DATE(R.string.no_due_date),
    DUE_THIS_WEEK(R.string.tasks_due_this_week);

    private final int stringResource;

    TaskDueDateGrouping(@StringRes int stringResource) {
        this.stringResource = stringResource;
    }

    @StringRes
    public int getStringResource() {
        return stringResource;
    }

    public static TaskDueDateGrouping convert(LocalDate taskDate) {
        LocalDate now = LocalDate.now();
        LocalDate tomorrow = now.plusDays(1);
        if (taskDate.isBefore(now)) {
            return OVERDUE;
        } else if (taskDate.isEqual(now)) {
            return TODAY;
        } else if (taskDate.isEqual(tomorrow)) {
            return TOMORROW;
        } else {
            return UPCOMING;
        }
    }
}
