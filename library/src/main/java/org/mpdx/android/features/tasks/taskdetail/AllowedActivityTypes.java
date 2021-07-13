package org.mpdx.android.features.tasks.taskdetail;

import org.mpdx.android.R;

import androidx.annotation.StringRes;

@SuppressWarnings("HardCodedStringLiteral")
public enum AllowedActivityTypes {
    CALL("Call", R.string.call),
    TEXT_MESSAGE("Text Message", R.string.text_message),
    FACEBOOK_MESSAGE("Facebook Message", R.string.facebook_message),
    EMAIL("Email", R.string.email);

    public final int stringRes;
    public final String apiValue;

    AllowedActivityTypes(String apiValue, @StringRes int stringRes) {
        this.apiValue = apiValue;
        this.stringRes = stringRes;
    }

    public int getStringRes() {
        return stringRes;
    }

    public String getApiValue() {
        return apiValue;
    }

    public static AllowedActivityTypes forApiValue(String apiValue) {
        if (apiValue == null) {
            return null;
        }
        for (AllowedActivityTypes type : values()) {
            if (type.apiValue.equals(apiValue)) {
                return type;
            }
        }
        return null;
    }
}
