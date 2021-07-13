package org.mpdx.android.utils;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import timber.log.Timber;

@SuppressWarnings("HardCodedStringLiteral")
public class DateUtils {
    private static final String UTC_CODE = "UTC";

    private static final SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    static {
        LONG_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone(UTC_CODE));
    }
    public static Calendar iterateBack(int position) {
        Calendar backDate = Calendar.getInstance();
        backDate.setTimeZone(TimeZone.getTimeZone(UTC_CODE));
        backDate.set(Calendar.DAY_OF_MONTH, 15); // Ignore actual day and use one that is in every month.
        backDate.add(Calendar.MONTH, -position);
        return backDate;
    }

    public static Date parseUTCFriendlyDate(String dateStr) {
        try {
            return LONG_DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            Timber.e(e);
            return null;
        }
    }

    public static boolean isMidnight(Date date) {
        final Calendar midnight = Calendar.getInstance();
        midnight.setTime(date);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        return date.getTime() == midnight.getTime().getTime();
    }

    public static LocalDate toLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
