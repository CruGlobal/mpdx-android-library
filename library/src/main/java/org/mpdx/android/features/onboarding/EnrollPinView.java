package org.mpdx.android.features.onboarding;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.features.secure.SecurityManager;

import androidx.core.content.res.ResourcesCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EnrollPinView extends LinearLayout {
    public static final int MIN_PIN_LENGTH = 4;
    private SecurityManager securityManager;

    @BindView(R2.id.enroll_pin_edittext) EditText enrollPinEditText;

    private EnrollPinCallback enrollPinCallback;

    public EnrollPinView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);

        setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.lock_background, null));

        enrollPinCallback = (EnrollPinCallback) context;

        LayoutInflater.from(context).inflate(R.layout.view_onboarding_enroll_pin, this, true);
        ButterKnife.bind(this);
    }

    public void setup(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @OnClick(R2.id.confirm_pin_button)
    public void onConfirmPinButtonClicked() {
        if (enrollPinEditText.getText().length() >= MIN_PIN_LENGTH) {
            boolean isPinValid = securityManager.enrollWithPin(enrollPinEditText.getText().toString());
            if (isPinValid) {
                enrollPinCallback.onPinEnrolled();
            } else {
                enrollPinCallback.onPinEnrollmentError();
            }
        } else {
            Toast.makeText(getContext(), R.string.invalid_pin, Toast.LENGTH_SHORT).show();
        }
    }
}
