package org.mpdx.android.utils;

import android.text.TextUtils;

import org.ccci.gto.android.common.compat.util.LocaleCompat;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * @deprecated Use the CurrencyUtils extension methods to format currency for display.
 */
@Deprecated
public class CurrencyFormatter {
    @Inject
    public CurrencyFormatter() {
    }

    public String formatForDevice(double amount, String isoCurrency) {
        return CurrencyUtilsKt.formatCurrency(amount, isoCurrency, getFormatLocale());
    }

    public String formatForCurrency(@Nullable final Double amount, String isoCurrency) {
        return CurrencyUtilsKt.formatCurrency(amount != null ? amount : 0, isoCurrency, getFormatLocale());
    }

    public String formatForCurrency(String amount, String isoCurrency) {
        if (amount == null) {
            amount = "0";
        }

        NumberFormat parser = NumberFormat.getNumberInstance();

        try {
            Number number = parser.parse(amount);
            return CurrencyUtilsKt.formatCurrency(number.doubleValue(), isoCurrency, getFormatLocale());
        } catch (ParseException e1) {
            return concat(amount, isoCurrency);
        } catch (IllegalArgumentException e2) {
            return concat(amount, isoCurrency);
        }
    }

    private String concat(String... values) {
        return TextUtils.join(" ", values);
    }

    public String formatGoal(int amount, String isoCurrency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(getFormatLocale());
        try {
            if (isoCurrency != null) {
                format.setCurrency(Currency.getInstance(isoCurrency));
            }
        } catch (IllegalArgumentException e) {
            Timber.e(e);
        }
        return format.format(amount);
    }

    public String formatPercent(double percent) {
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMaximumFractionDigits(2);
        return nf.format(percent);
    }

    private Locale getFormatLocale() {
        return LocaleCompat.getDefault(LocaleCompat.Category.FORMAT);
    }

    public String formatNumber(int number) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        return  nf.format(number);
    }

    public String formatNumber(long number) {
        NumberFormat nf = NumberFormat.getNumberInstance(getFormatLocale());
        return  nf.format(number);
    }
}
