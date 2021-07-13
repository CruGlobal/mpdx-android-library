package org.mpdx.android.features.base;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.trello.rxlifecycle2.android.ActivityEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;
import org.mpdx.android.R;
import org.mpdx.android.base.AppConstantListener;
import org.mpdx.android.base.AuthenticationListener;
import org.mpdx.android.core.modal.ModalActivity;
import org.mpdx.android.core.model.AccountList;
import org.mpdx.android.core.model.AccountListFields;
import org.mpdx.android.core.realm.RealmManager;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.MainActivity;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.secure.UnlockFragment;
import org.mpdx.android.utils.NetUtils;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import butterknife.ButterKnife;
import io.reactivex.subjects.BehaviorSubject;
import io.realm.Realm;
import timber.log.Timber;

import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_ON_BOARDING_SIGN_IN;

public abstract class BaseActivity extends AppCompatActivity {
    public static final String EXTRA_DEEP_LINK_JSON = "data";
    public static final String EXTRA_DEEP_LINK_TYPE = "org.mpdx.android.features.base.BaseActivity.deep_link_type";
    public static final String EXTRA_DEEP_LINK_ID = "org.mpdx.android.features.base.BaseActivity.deep_link_id";
    public static final String EXTRA_DEEP_LINK_TIME = "org.mpdx.android.features.base.BaseActivity.deep_link_time";
    public static final String EXTRA_DEEP_LINK_ACCOUNT_LIST_ID = "account_list_id";
    public static final String EXTRA_DEEP_LINK_CONTACT_URL = "contact_url";
    public static final String DEEP_LINK_TYPE_TASK = "task";
    public static final String DEEP_LINK_TYPE_CONTACTS = "contacts";
    public static final String DEEP_LINK_TYPE_COACHING = "coaching";

    @Inject protected AppPrefs appPrefs;
    @Inject protected EventBus mEventBus;
    @Inject protected RealmManager mRealmManager;
    @Inject protected AuthenticationListener authenticationListener;
    @Inject protected AppConstantListener constantListener;
    private final BehaviorSubject<ActivityEvent> lifecycleSubject = BehaviorSubject.create();

    protected String deepLinkType;
    protected String deepLinkAccountList;
    protected String deepLinkId;
    protected long deepLinkTime;
    protected boolean deepLinkIsOtherAccount = false;

    @Override
    @CallSuper
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (appPrefs != null && appPrefs.hasCompletedOnboarding() && showUnlockScreen()) {
            ModalActivity.launchActivity(this, UnlockFragment.create(deepLinkType, deepLinkId, deepLinkTime));

            savedInstanceState = null;

            finish();
        }

        super.onCreate(savedInstanceState);
        extractDeepLinkArguments();

        setupUI();
        lifecycleSubject.onNext(ActivityEvent.CREATE);
    }

    protected void startLoginActivity(BaseActivity activity) {
        mEventBus.post(new AnalyticsScreenEvent(SCREEN_ON_BOARDING_SIGN_IN));
        authenticationListener.logIntoSession(this);
    }

    protected void extractDeepLinkArguments() {
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null) {
            List<String> pathSegments = data.getPathSegments();
            if (pathSegments.size() < 2) {
                return;
            }
            deepLinkType = pathSegments.get(0);
            deepLinkId = pathSegments.get(1);
        } else if (intent != null && intent.getStringExtra(EXTRA_DEEP_LINK_TYPE) != null) {
            deepLinkType = intent.getStringExtra(EXTRA_DEEP_LINK_TYPE);
            deepLinkId = intent.getStringExtra(EXTRA_DEEP_LINK_ID);
            deepLinkTime = intent.getLongExtra(EXTRA_DEEP_LINK_TIME, 0L);
        } else if (intent != null && intent.getStringExtra(EXTRA_DEEP_LINK_JSON) != null) {
            try {
                JSONObject obj = new JSONObject(intent.getStringExtra(EXTRA_DEEP_LINK_JSON));
                intent.removeExtra(EXTRA_DEEP_LINK_JSON);
                String url = obj.getString("contact_url");
                int startIndex = url.lastIndexOf("/", url.length() - 2);
                String id = url.substring(startIndex + 1);
                String accountListId = obj.getString("account_list_id");

                startDeepLinkLogin(id, accountListId);

            } catch (JSONException e) {
                Timber.e(e);
            }
        } else if (intent != null && intent.getStringExtra(EXTRA_DEEP_LINK_ACCOUNT_LIST_ID) != null) {
            String url = intent.getStringExtra(EXTRA_DEEP_LINK_CONTACT_URL);
            intent.removeExtra(EXTRA_DEEP_LINK_JSON);
            String id = null;
            if (url != null) {
                int startIndex = url.lastIndexOf("/", url.length() - 2);
                id = url.substring(startIndex + 1);
            }
            String accountListId = intent.getStringExtra(EXTRA_DEEP_LINK_ACCOUNT_LIST_ID);

            startDeepLinkLogin(id, accountListId);
        }
    }

    private void startDeepLinkLogin(String id, String accountListId) {
        deepLinkType = DEEP_LINK_TYPE_CONTACTS;
        deepLinkId = id;
        deepLinkAccountList = accountListId;

        if (deepLinkAccountList != null && appPrefs.getAccountListId() != null &&
                !deepLinkAccountList.equals(appPrefs.getAccountListId())) {
            deepLinkIsOtherAccount = true;

            new AlertDialog.Builder(this)
                    .setMessage(R.string.change_account_list_notification_dialog)
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        startActivity(MainActivity.getIntent(
                                BaseActivity.this, deepLinkType, deepLinkId));
                        deepLinkIsOtherAccount = false;
                    })
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        boolean accountUnderSameUser = changeAccountList(deepLinkAccountList);
                        if (!accountUnderSameUser) {
                            startLoginActivity(BaseActivity.this);
                        } else {
                            ModalActivity.launchActivity(BaseActivity.this,
                                    UnlockFragment.create(deepLinkType, deepLinkId, deepLinkTime),
                                    true);
                        }
                        deepLinkIsOtherAccount = false;
                    })
                    .setOnCancelListener(dialog -> {
                        startActivity(MainActivity.getIntent(
                                BaseActivity.this, deepLinkType, deepLinkId));
                        deepLinkIsOtherAccount = false;
                    })
                    .show();

        }
    }

    private boolean changeAccountList(String accountListId) {
        AccountList list;
        try (Realm realm = Realm.getDefaultInstance()) {
            list = realm.where(AccountList.class).equalTo(AccountListFields.ID, accountListId).findFirst();
        }
        if (list != null) {
            mRealmManager.deleteRealm(false);
            appPrefs.setAccountListId(accountListId);
            return true;
        } else {
            mRealmManager.deleteRealm(false);
            appPrefs.logoutUser(this);
            return false;
        }
    }

    protected boolean showUnlockScreen() {
        return !mRealmManager.isUnlocked();
    }

    protected void setupUI() {
        final int layout = layoutId();
        if (layout != 0) {
            setContentView(layout);
            ButterKnife.bind(this);
        }
    }

    protected void setupToolbar() {
        if (getSupportActionBar() != null) {
            //TODO: Change to X icon when we have asset
            final Drawable upArrowDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.cru_icon_up);
            getSupportActionBar().setHomeAsUpIndicator(upArrowDrawable);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    protected void launchHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.help_available_options);
        builder.setItems(new String[] {getString(R.string.help_option_web), getString(R.string.help_option_email)},
            (dialog, which) -> {
                switch (which) {
                    case 0:
                        launchHelpSite();
                        break;
                    case 1:
                        launchHelpEmail();
                        break;
                }
            });
        builder.show();
    }

    protected void launchHelpSite() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://help.mpdx.org/collection/490-mpdx-mobile-help"));
        startActivity(browserIntent);
    }

    protected void launchHelpEmail() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@mpdx.org"});

        StringBuffer emailBody = new StringBuffer(200);
        emailBody.append("\n\n\n\n\n\n\n\n\n\nCurrent Page: ").append(getPageName())
                .append("\nDevice Type: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append("\nOS: ").append(Build.VERSION.CODENAME).append(' ').append(Build.VERSION.SDK_INT)
                .append("\nApp Version: ").append(constantListener.buildVersion())
                .append("\nIP Address: ").append(NetUtils.getIPAddress())
                .append("\nUser ID: ").append(appPrefs.getUserId())
                .append("\nAccount List ID: ").append(appPrefs.getAccountListId());

        intent.putExtra(Intent.EXTRA_TEXT, emailBody.toString());
        startActivity(Intent.createChooser(intent, getString(R.string.email_app_selection)));
    }

    @Override
    @CallSuper
    protected void onStart() {
        super.onStart();
        lifecycleSubject.onNext(ActivityEvent.START);
    }

    @Override
    @CallSuper
    protected void onResume() {
        super.onResume();
        lifecycleSubject.onNext(ActivityEvent.RESUME);
    }

    @Override
    @CallSuper
    protected void onPause() {
        lifecycleSubject.onNext(ActivityEvent.PAUSE);
        super.onPause();
    }

    @Override
    @CallSuper
    protected void onStop() {
        lifecycleSubject.onNext(ActivityEvent.STOP);
        super.onStop();
    }

    @Override
    @CallSuper
    protected void onDestroy() {
        lifecycleSubject.onNext(ActivityEvent.DESTROY);
        super.onDestroy();
    }

    protected String getPageName() {
        return getClass().getSimpleName();
    }

    @LayoutRes
    public int layoutId() {
        return 0;
    }
}
