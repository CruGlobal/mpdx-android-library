package org.mpdx.android.features.settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.base.AppConstantListener;
import org.mpdx.android.core.modal.ModalActivity;
import org.mpdx.android.core.modal.ModalFragment;
import org.mpdx.android.core.realm.RealmManager;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.MainActivity;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.analytics.model.SettingsLogOutAnalyticsEvent;
import org.mpdx.android.features.base.fragments.BaseFragment;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import butterknife.BindView;
import butterknife.OnClick;
import dagger.hilt.android.AndroidEntryPoint;

import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_SETTINGS;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_SETTINGS_PRIVACY_POLICY;

@AndroidEntryPoint
public class SettingsFragment extends BaseFragment implements ModalFragment {
    private static final int REQUEST_ACCOUNT_LIST = 2;
    @BindView(R2.id.settings_version_number) TextView version;
    @BindView(R2.id.account_list_id) TextView accountListId;

    @Inject AppPrefs appPrefs;
    @Inject RealmManager mRealmManager;
    @Inject AppConstantListener appConstantListener;

    private SettingsFragmentViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsFragmentViewModel.class);
    }

    @SuppressLint({"CheckResult", "DefaultLocale"})
    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        version.setText(String.format("%s (%d)", appConstantListener.buildVersion(), appConstantListener.versionCode()));
        viewModel.getAccountList().observe(getViewLifecycleOwner(), accountList -> {
            if (accountList != null && !TextUtils.isEmpty(accountList.getName())) {
                accountListId.setText(accountList.getName());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mEventBus.post(new AnalyticsScreenEvent(SCREEN_SETTINGS));
    }

    @Override
    public Toolbar getToolbar() {
        return null;
    }

    @Override
    protected int layoutRes() {
        return R.layout.fragment_settings;
    }

    @OnClick(R2.id.logout)
    void logout() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.logout_dialog)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    mRealmManager.deleteRealm(false);
                    appPrefs.logoutUser(requireActivity());

                    mEventBus.post(SettingsLogOutAnalyticsEvent.INSTANCE);
                })
                .show();
    }

    @OnClick(R2.id.account_list_id_container)
    void toAccountListSelector() {
        ModalActivity.launchActivityForResult(requireActivity(), new SetAccountListFragment(), REQUEST_ACCOUNT_LIST);
    }

    @OnClick(R2.id.notification_settings)
    void toNotificationSettings() {
        ModalActivity.launchActivity(requireActivity(), new NotificationSettingsFragment());
    }

    @OnClick(R2.id.privacy_policy)
    void toPrivacyPolicy() {
        requireContext().startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cru.org/us/en/about/privacy.html")));
        mEventBus.post(new AnalyticsScreenEvent(SCREEN_SETTINGS_PRIVACY_POLICY));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == MainActivity.RESULT_CLOSE_ACTIVITY) {
            requireActivity().setResult(MainActivity.RESULT_CLOSE_ACTIVITY);
        }
    }
}
