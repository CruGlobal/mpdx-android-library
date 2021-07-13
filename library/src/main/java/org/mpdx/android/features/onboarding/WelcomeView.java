package org.mpdx.android.features.onboarding;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import org.mpdx.android.R;
import org.mpdx.android.R2;

import androidx.core.content.res.ResourcesCompat;
import butterknife.ButterKnife;
import butterknife.OnClick;

class WelcomeView extends LinearLayout {

    private GetStartedCallback callback;

    WelcomeView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.lock_background, null));

        this.callback = (GetStartedCallback) context;

        LayoutInflater.from(context).inflate(R.layout.view_onboarding_welcome, this, true);
        ButterKnife.bind(this);
    }

    @OnClick(R2.id.get_started_button)
    public void getStartedClicked() {
        callback.onGetStarted();
    }
}
