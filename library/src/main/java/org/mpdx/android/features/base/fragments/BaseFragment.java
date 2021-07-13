package org.mpdx.android.features.base.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import butterknife.ButterKnife;

/**
 * By default supports setting up injection, additionally attempts to bind (using {@link BaseFragment#bindView(View)})
 * the fragment to the inflated view determined by {@link BaseFragment#layoutRes()}
 * you can override the binding behavior example being {@link BindingFragment#bindView(View)}
 */
public abstract class BaseFragment extends Fragment  {

    public static final String ARG_DEEP_LINK_TYPE = "deep_link_type";
    public static final String ARG_DEEP_LINK_ID = "deep_link_id";
    public static final String ARG_DEEP_LINK_TIME = "deep_link_time";

    @Inject protected EventBus mEventBus;

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        ViewCompat.requestApplyInsets(view);
        return view;
    }

    protected View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(layoutRes(), container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    public void setDeepLinkId(String id, long time) {
        setDeepLinkArguments(null, id, time);
    }

    protected String getDeepLinkId() {
        if (getArguments() != null) {
            return getArguments().getString(ARG_DEEP_LINK_ID);
        }
        return null;
    }

    protected long getDeepLinkTime() {
        if (getArguments() != null) {
            return getArguments().getLong(ARG_DEEP_LINK_TIME);
        }
        return 0;
    }

    protected void setDeepLinkArguments(String type, String id, long time) {
        Bundle args = new Bundle();
        args.putString(ARG_DEEP_LINK_TYPE, type);
        args.putString(ARG_DEEP_LINK_ID, id);
        args.putLong(ARG_DEEP_LINK_TIME, time);
        setArguments(args);
    }

    protected void clearDeepLink() {
        Bundle args = getArguments();
        if (args != null) {
            args.remove(ARG_DEEP_LINK_TYPE);
            args.remove(ARG_DEEP_LINK_ID);
        }
    }

    @LayoutRes
    protected abstract int layoutRes();

    protected AppCompatActivity getSupportActivity() {
        return (AppCompatActivity) requireActivity();
    }
}
