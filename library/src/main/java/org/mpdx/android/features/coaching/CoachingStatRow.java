package org.mpdx.android.features.coaching;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.mpdx.android.R;
import org.mpdx.android.features.coaching.model.stats.CoachingStat;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Map;

import androidx.core.content.ContextCompat;

public class CoachingStatRow extends LinearLayout {
    private Currency currency;

    public CoachingStatRow(Context context, CoachingStat stat) {
        this(context, stat, null);
    }

    public CoachingStatRow(Context context, CoachingStat stat, String currency) {
        super(context);

        inflate(context, R.layout.coaching_stat_row, this);

        ((ImageView) findViewById(R.id.coaching_stat_icon)).setImageDrawable(
                ContextCompat.getDrawable(context, stat.getDrawable()));
        ((TextView) findViewById(R.id.coaching_stat_label)).setText(stat.getLabel());

        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout colContainer = findViewById(R.id.coaching_stat_col_container);
        Map map = stat.getMap();
        NumberFormat formatter = NumberFormat.getInstance();
        for (Object entry : map.entrySet()) {
            Object labelRes = ((Map.Entry) entry).getKey();
            Object value = ((Map.Entry) entry).getValue();

            View col = inflater.inflate(R.layout.coaching_stat_col, colContainer, false);
            ((TextView) col.findViewById(R.id.stat_col_value)).setText(formatter.format(value));
            if (currency != null) {
                ((TextView) col.findViewById(R.id.stat_col_label)).setText(currency.toLowerCase());
            } else if ((labelRes instanceof Integer)) {
                ((TextView) col.findViewById(R.id.stat_col_label)).setText((Integer) labelRes);
            } else {
                ((TextView) col.findViewById(R.id.stat_col_label)).setText((String) labelRes);
            }
            colContainer.addView(col);
        }
    }

}
