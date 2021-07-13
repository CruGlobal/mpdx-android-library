package org.mpdx.android.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

public class StringResolver {

    private Context context;

    public StringResolver(Context context) {
        this.context = context;
    }

    @NonNull
    public String getString(@StringRes int stringResId) {
        return context.getString(stringResId);
    }

    public String getString(@StringRes int stringResId, Object... args) {
        return context.getString(stringResId, args);
    }

    public String getQuantityString(@PluralsRes int stringResId, int quantity) {
        return context.getResources().getQuantityString(stringResId, quantity, quantity);
    }
}
