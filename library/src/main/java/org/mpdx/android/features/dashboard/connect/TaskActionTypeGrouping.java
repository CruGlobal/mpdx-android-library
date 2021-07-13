package org.mpdx.android.features.dashboard.connect;

import org.mpdx.android.R;

import androidx.annotation.StringRes;
import androidx.core.util.ObjectsCompat;
import timber.log.Timber;

@SuppressWarnings("HardCodedStringLiteral")
public enum TaskActionTypeGrouping {
    CALL(R.string.call, "Call"),
    LETTER(R.string.letter, "Letter"),
    NEWSLETTER_PHYSICAL(R.string.newsletter_physical, "Newsletter - Physical"),
    NEWSLETTER_EMAIL(R.string.newsletter_email, "Newsletter - Email"),
    PRE_CALL_LETTER(R.string.pre_call_letter, "Pre Call Letter"),
    REMINDER_LETTER(R.string.reminder_letter, "Reminder Letter"),
    SUPPORT_LETTER(R.string.support_letter, "Support Letter"),
    THANK(R.string.thank, "Thank"),
    PRAYER_REQUEST(R.string.prayer_request, "Prayer Request"),
    APPOINTMENT(R.string.appointment, "Appointment"),
    FACEBOOK_MESSAGE(R.string.facebook_message, "Facebook Message"),
    EMAIL(R.string.email, "Email"),
    TODO(R.string.todo, "To Do"),
    TALK_TO_IN_PERSON(R.string.talk_to_in_person, "Talk to In Person"),
    TEXT_MESSAGE(R.string.text_message, "Text Message"),
    NO_ACTION_SET(R.string.no_action_set, "No Action Set");

    private final int stringResource;
    private final String apiLabel;

    TaskActionTypeGrouping(@StringRes int stringResource, String apiLabel) {
        this.stringResource = stringResource;
        this.apiLabel = apiLabel;
    }

    @StringRes
    public int getStringResource() {
        return stringResource;
    }

    public String getApiLabel() {
        return apiLabel;
    }

    public static TaskActionTypeGrouping fromCode(String apiLabel) {
        if (apiLabel == null) {
            return NO_ACTION_SET;
        }
        for (TaskActionTypeGrouping value : values()) {
            if (ObjectsCompat.equals(value.apiLabel, apiLabel)) {
                return value;
            }
        }
        Timber.e("Invalid label: %s", apiLabel);
        return null;
    }

}
