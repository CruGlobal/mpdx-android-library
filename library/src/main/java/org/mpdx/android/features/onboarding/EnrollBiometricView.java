package org.mpdx.android.features.onboarding;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import org.mpdx.android.R;
import org.mpdx.android.R2;

import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;
import butterknife.ButterKnife;
import butterknife.OnClick;

@RequiresApi(api = Build.VERSION_CODES.M)
public class EnrollBiometricView extends LinearLayout {
    private EnrollBiometricCallback enrollBiometricCallback;

    public EnrollBiometricView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.biometric_background, null));

        enrollBiometricCallback = (EnrollBiometricCallback) context;

        LayoutInflater.from(context).inflate(R.layout.view_onboarding_enroll_biometric, this, true);
        ButterKnife.bind(this);
    }

    @OnClick(R2.id.fingerprint_skip)
    public void onFingerprintSkipButtonClicked() {
        enrollBiometricCallback.onBiometricSkip();
    }
}
