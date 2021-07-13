package org.mpdx.android.features.dashboard.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.mpdx.android.R;
import org.mpdx.android.R2;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ItemPairView extends CardView {

    @BindView(R2.id.image) ImageView imageView;
    @BindView(R2.id.text) TextView textView;

    public ItemPairView(Context context) {
        super(context);
        init();
    }

    public ItemPairView(Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ItemPairView(Context context,
            @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.v_item_pair, this);
        ButterKnife.bind(this, this);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        params.bottomMargin = 1;
        setLayoutParams(params);
    }

    public ImageView getImageView() {
        return imageView;
    }

    public TextView getTextView() {
        return textView;
    }
}
