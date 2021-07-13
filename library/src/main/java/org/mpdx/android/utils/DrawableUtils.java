package org.mpdx.android.utils;

import androidx.annotation.DrawableRes;

import org.ccci.gto.android.common.base.Constants;
import org.mpdx.android.R;

public class DrawableUtils {
    @DrawableRes
    @SuppressWarnings("HardCodedStringLiteral")
    public static int getDrawableForActivity(String activity) {
        if (activity == null) {
            return R.drawable.ic_chevron_right;
        }
        switch (activity) {
            case "Text Message":
                return R.drawable.cru_icon_message;
            case "Call":
                return R.drawable.cru_icon_phone;
            case "Appointment":
                return R.drawable.cru_icon_calendar;
            case "Email":
                return R.drawable.cru_icon_mail;
            case "Facebook Message":
                return R.drawable.cru_icon_facebook;
            case "To Do":
                return R.drawable.cru_icon_note;
            case "Talk to In Person":
                return R.drawable.cru_icon_person;
            case "Letter":
            case "Newsletter - Physical":
            case "Newsletter - Email":
            case "Pre Call Letter":
            case "Reminder Letter":
            case "Support Letter":
                return R.drawable.cru_icon_letter;
            case "Prayer Request":
            case "Thank":
                return R.drawable.cru_icon_pencil;
            default:
                return Constants.INVALID_DRAWABLE_RES;
        }
    }
}
