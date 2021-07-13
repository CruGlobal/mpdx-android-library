package org.mpdx.android.features.contacts.people;

import org.mpdx.android.R;

import androidx.annotation.StringRes;

public enum PeopleGrouping {
    BIRTHDAYS_THIS_WEEK(R.string.birthdays_this_week_label);

    private final int stringResource;

    PeopleGrouping(@StringRes int stringResource) {
        this.stringResource = stringResource;
    }

    @StringRes
    public int getStringResource() {
        return stringResource;
    }

}
