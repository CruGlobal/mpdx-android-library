package org.mpdx.android.features.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.analytics.model.AnalyticsActionEvent;
import org.mpdx.android.features.base.BaseActivity;
import org.mpdx.android.features.secure.BiometricCallback;
import org.mpdx.android.features.secure.BiometricErrorDialogFragment;
import org.mpdx.android.features.secure.SecurityManager;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricPrompt;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

import static org.mpdx.android.features.analytics.model.AnalyticsActionEventKt.ACTION_LOCK_METHOD;
import static org.mpdx.android.features.analytics.model.AnalyticsActionEventKt.CATEGORY_SETUP;
import static org.mpdx.android.features.analytics.model.AnalyticsActionEventKt.LABEL_FINGERPRINT;
import static org.mpdx.android.features.analytics.model.AnalyticsActionEventKt.LABEL_PIN;

@AndroidEntryPoint
public class OnboardingActivity extends BaseActivity implements ViewPager.OnPageChangeListener, GetStartedCallback,
        EnrollPinCallback, BiometricCallback, EnrollBiometricCallback {

    @Inject SecurityManager securityManager;
    @Inject AppPrefs appPrefs;

    @BindView(R2.id.onboarding_pager) ViewPager viewPager;

    public static void startActivity(Activity activity) {
        activity.startActivity(new Intent(activity, OnboardingActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OnboardingPagerAdapter pagerAdapter = new OnboardingPagerAdapter(this, securityManager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(this);

        onPageSelected(0);
    }

    @Override
    protected void onDestroy() {
        securityManager.cancelListeningForBiometric();
        super.onDestroy();
    }

    @Override
    public int layoutId() {
        return R.layout.activity_onboarding;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (position == OnboardingPagesEnum.ENROLL_BIOMETRIC.ordinal()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                viewPager.findViewWithTag(OnboardingPagesEnum.ENROLL_BIOMETRIC)
                        .findViewById(R.id.start_biometric).setOnClickListener(v ->
                        securityManager.startListeningForBiometrics(
                                OnboardingActivity.this,
                                OnboardingActivity.this,
                                Cipher.ENCRYPT_MODE));
            }
        } else {
            securityManager.cancelListeningForBiometric();
        }

        if (position != OnboardingPagesEnum.ENROLL_PIN.ordinal()) {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onGetStarted() {
        advancePager();
    }

    @Override
    public void onPinEnrolled() {
        if (securityManager.deviceHasBiometricSupport()) {
            advancePager();
        } else {
            // Skip over fingerprint enrollment
            mEventBus.post(new AnalyticsActionEvent(ACTION_LOCK_METHOD, CATEGORY_SETUP, LABEL_PIN));
            completeOnboarding();
        }
    }

    @Override
    public void onPinEnrollmentError() {
        Timber.e("PIN enrollment error");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBiometricSuccess(BiometricPrompt.AuthenticationResult result) {
        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
        EnrollBiometricView view = viewPager.findViewWithTag(OnboardingPagesEnum.ENROLL_BIOMETRIC);
        EditText pin = view.findViewById(R.id.pin_edittext);
        String pinText = pin.getText().toString();
        if (TextUtils.isEmpty(pinText)) {
            Toast.makeText(this, R.string.incorrect_pin, Toast.LENGTH_SHORT).show();
            securityManager.startListeningForBiometrics(this, this, Cipher.ENCRYPT_MODE);
            return;
        }

        try {
            boolean isEnrolled = securityManager.enrollWithBiometric(pinText);
            if (isEnrolled) {
                Toast.makeText(this, R.string.fingerprint_enrolled, Toast.LENGTH_SHORT).show();
                mEventBus.post(new AnalyticsActionEvent(ACTION_LOCK_METHOD, CATEGORY_SETUP, LABEL_FINGERPRINT));
                completeOnboarding();
            } else {
                Timber.e("Fingerprint enrollment error");
                BiometricErrorDialogFragment dialogFragment = new BiometricErrorDialogFragment();
                dialogFragment.setListener(new BiometricErrorDialogFragment.BiometricErrorDialogListener() {
                    @Override
                    public void onRetryClicked() {
                        securityManager.startListeningForBiometrics(OnboardingActivity.this, OnboardingActivity.this, Cipher.ENCRYPT_MODE);
                    }

                    @Override
                    public void onSkipClicked() {
                        completeOnboarding();
                    }
                });
                dialogFragment.show(getSupportFragmentManager(), BiometricErrorDialogFragment.class.getName());
            }
        } catch (BadPaddingException e) {
            Timber.i(e);
            Toast.makeText(this, R.string.incorrect_pin, Toast.LENGTH_SHORT).show();
            securityManager.startListeningForBiometrics(this, this, Cipher.ENCRYPT_MODE);
        }
    }

    private void advancePager() {
        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
    }

    private void completeOnboarding() {
        appPrefs.setCompletedOnboarding(true);
        finish();
    }

    @Override
    public void onBiometricError(int errorCode, @NonNull String errString) {
        Timber.e("Fingerprint enrollment error: %d %s", errorCode, errString);
        Toast.makeText(this, errString, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBiometricFailed() {
        Timber.e("Fingerprint enrollment failed");
        Toast.makeText(this, R.string.fingerprint_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected boolean showUnlockScreen() {
        return false;
    }

    @Override
    public void onBiometricSkip() {
        completeOnboarding();
    }

    @Override
    protected String getPageName() {
        return "Onboarding";
    }
}
