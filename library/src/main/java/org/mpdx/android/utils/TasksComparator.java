package org.mpdx.android.utils;

import org.mpdx.android.features.tasks.model.Task;

import java.util.Comparator;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by Ranabhat on 11/8/17.
 * Updated by ryancarlson on 12/13/17.
 *
 * Sort criteria go as follows:
 *
 * 1. Sort by Date: latest first, nulls last
 * 2. When dates are equal, sort by number of contacts: multiple contacts, followed by one contact, followed by no contacts
 * 3. When number of contacts are equal, sort my name of contact alphabetically
 */

@Singleton
public class TasksComparator implements Comparator<Task> {
    private static final int SORT_ORDER_NO_CONTACTS = 2;
    private static final int SORT_ORDER_MULTIPLE_CONTACTS = 0;
    private static final int SORT_ORDER_ONE_CONTACT = 1;

    private StringResolver stringResolver;

    @Inject
    public TasksComparator(StringResolver stringResolver) {
        this.stringResolver = stringResolver;
    }

    @Override
    public int compare(Task left, Task right) {
        if (left.getStartAt() == null && right.getStartAt() == null) {
            return compareContactNames(left, right);
        }
        if (left.getStartAt() == null) {
            return 1;
        }
        if (right.getStartAt() == null) {
            return -1;
        }

        int dateComparisonResult = right.getStartAt().compareTo(left.getStartAt());

        if (dateComparisonResult != 0) {
            return dateComparisonResult;
        }

        return compareContactNames(left, right);
    }

    private int compareContactNames(Task left, Task right) {
            int contactComparison = TextUtils.compare(left.getContactNameText(stringResolver), right.getContactNameText(stringResolver));

            if (contactComparison == 0) {
                return TextUtils.compare(left.getDescription(stringResolver), right.getDescription(stringResolver));
            } else {
                return contactComparison;
            }
    }
}
