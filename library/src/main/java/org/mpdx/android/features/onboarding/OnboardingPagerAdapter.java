package org.mpdx.android.features.onboarding;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import org.mpdx.android.features.secure.SecurityManager;

import androidx.viewpager.widget.PagerAdapter;

public class OnboardingPagerAdapter extends PagerAdapter {

    private Activity activity;
    private SecurityManager securityManager;

    public OnboardingPagerAdapter(Activity context, SecurityManager securityManager) {
        this.activity = context;
        this.securityManager = securityManager;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        OnboardingPagesEnum onboardingPagesEnum = OnboardingPagesEnum.values()[position];
        View layout;
        switch (onboardingPagesEnum) {
            case WELCOME:
                layout = new WelcomeView(activity);
                layout.setTag(OnboardingPagesEnum.WELCOME);
                break;
            case ENROLL_PIN:
                layout = new EnrollPinView(activity);
                layout.setTag(OnboardingPagesEnum.ENROLL_PIN);
                ((EnrollPinView) layout).setup(securityManager);
                break;
            case ENROLL_BIOMETRIC:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    layout = new EnrollBiometricView(activity);
                    layout.setTag(OnboardingPagesEnum.ENROLL_BIOMETRIC);
                } else {
                    return null;
                }
                break;
            default:
                return null;
        }
        container.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object view) {
        container.removeView((View) view);
    }

    @Override
    public int getCount() {
        return OnboardingPagesEnum.values().length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }
}
