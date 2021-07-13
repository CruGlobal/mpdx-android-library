package org.mpdx.android.features.notifications.list;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.features.notifications.realm.NotificationQueriesKt;
import org.mpdx.android.utils.MenuItemUtilsKt;
import org.mpdx.android.core.modal.ModalActivity;
import org.mpdx.android.core.modal.ModalFragment;
import org.mpdx.android.core.model.AccountList;
import org.mpdx.android.core.realm.RealmManager;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.analytics.model.FilterAppliedAnalyticsEvent;
import org.mpdx.android.features.base.fragments.BaseFragment;
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivity;
import org.mpdx.android.features.contacts.model.Contact;
import org.mpdx.android.features.filter.model.Filter;
import org.mpdx.android.features.filter.repository.FiltersRepository;
import org.mpdx.android.features.filter.selector.FilterSelectorFragment;
import org.mpdx.android.features.notifications.model.Notification;
import org.mpdx.android.features.notifications.model.UserNotification;
import org.mpdx.android.features.notifications.sync.NotificationsSyncService;
import org.mpdx.android.features.secure.UnlockFragment;

import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import dagger.hilt.android.AndroidEntryPoint;
import io.realm.Realm;
import io.realm.RealmResults;

import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_NOTIFICATION_ALL;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_NOTIFICATION_UNREAD;
import static org.mpdx.android.features.base.BaseActivity.DEEP_LINK_TYPE_CONTACTS;

@AndroidEntryPoint
public class NotificationsFragment extends BaseFragment implements ModalFragment, OnNotificationSelectedListener {
    private static final int SELECTION_UNREAD = 0;
    private static final int SELECTION_ALL = 1;

    @Inject FiltersRepository filterRepository;
    @Inject AppPrefs appPrefs;
    @Inject NotificationsSyncService mNotificationsSyncService;
    @Inject RealmManager mRealmManager;
    private Realm mRealm;

    @BindView(R2.id.notifications_toolbar) Toolbar toolbar;
    @BindView(R2.id.notifications_tabs) TabLayout tabLayout;
    private MenuItem mFilterAction;

    private NotificationsFragmentViewModel viewModel;

    public static Fragment create(String id, long deepLinkTime) {
        NotificationsFragment fragment = new NotificationsFragment();
        fragment.setDeepLinkId(id, deepLinkTime);
        return fragment;
    }

    // region Lifecycle Events

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mRealm = Realm.getDefaultInstance();
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        viewModel = new ViewModelProvider(this).get(NotificationsFragmentViewModel.class);
        viewModel.syncData(false);
        setupFilterBadgeText();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getSupportActivity().setSupportActionBar(toolbar);
        toolbar.setTitle(getString(R.string.notifications).toLowerCase(Locale.getDefault()));

        TabLayout.Tab unreadTab = tabLayout.newTab();
        unreadTab.setText(R.string.notification_tab_unread);
        tabLayout.addTab(unreadTab);

        TabLayout.Tab allTab = tabLayout.newTab();
        allTab.setText(R.string.notification_tab_all);
        tabLayout.addTab(allTab);

        setupSwipeRefreshLayout();
        setupRecyclerView();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadFromRealm(tab.getPosition() == SELECTION_UNREAD);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                loadFromRealm(tab.getPosition() == SELECTION_UNREAD);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            filterRepository.updateNotificationFilters();
            Fragment fragment =
                    new FilterSelectorFragment(Filter.CONTAINER_NOTIFICATION, Filter.Type.NOTIFICATION_TYPES);
            ModalActivity.launchActivity(requireActivity(), fragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNotificationSelected(@Nullable final UserNotification userNotification) {
        if (userNotification == null) {
            return;
        }
        markNotificationRead(userNotification);
        final Notification notification = userNotification.getNotification();
        final Contact contact = notification != null ? notification.getContact() : null;
        if (contact == null) {
            return;
        }
        final AccountList accountList = contact.getAccountList();
        final String accountListId = accountList != null ? accountList.getId() : null;
        if (accountListId == null || accountListId.equals(appPrefs.getAccountListId())) {
            startActivity(ContactDetailActivity.getIntent(getActivity(), contact.getId()));
        } else {
            new AlertDialog.Builder(requireContext())
                    .setMessage(R.string.loading_account_list_for_contact)
                    .setPositiveButton(R.string.yes, (d, w) -> changeAccountList(accountListId, contact.getId()))
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    @Override
    public void onDestroyView() {
        cleanupRecyclerView();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    // endregion Lifecycle Events
    @Override
    protected int layoutRes() {
        return R.layout.fragment_notifications;
    }

    @Override
    public Toolbar getToolbar() {
        return null;
    }

    private void markNotificationRead(@NonNull final UserNotification notification) {
        final String notificationId = notification.getId();
        mRealm.executeTransactionAsync(realm -> {
            final UserNotification fresh =
                    NotificationQueriesKt.getUserNotification(realm, notificationId).findFirst();
            if (fresh != null) {
                fresh.setTrackingChanges(true);
                fresh.setRead(true);
            }
        }, mNotificationsSyncService.syncDirtyNotifications()::launch);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.notifications_list_fragment_menu, menu);
        mFilterAction = menu.findItem(R.id.action_filter);
        updateFilterBadgeText();
    }

    // region Filter Number Badge
    private void setupFilterBadgeText() {
        viewModel.getFilters().observe(this, a -> updateFilterBadgeText());
    }

    private void updateFilterBadgeText() {
        final RealmResults<Filter> filters = viewModel.getFilters().getValue();
        final int size = filters != null ? filters.size() : 0;
        if (size > 0) {
            mEventBus.post(FilterAppliedAnalyticsEvent.INSTANCE);
        }
        if (mFilterAction != null) {
            MenuItemUtilsKt.updateBadgeNumber(
                    mFilterAction, new ContextThemeWrapper(requireContext(), R.style.Theme_Mpdx_Filters_Action),
                    size);
        }
    }
    // endregion Filter Number Badge

    // region SwipeRefreshLayout
    @BindView(R2.id.notifications_refresh)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private void setupSwipeRefreshLayout() {
        mSwipeRefreshLayout.setOnRefreshListener(() -> viewModel.syncData(true));
        viewModel.getSyncTracker().isSyncing().observe(getViewLifecycleOwner(), state -> {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(state);
            }
        });
    }
    // endregion SwipeRefreshLayout

    // region RecyclerView
    @Nullable
    @BindView(R2.id.notifications_recycler) RecyclerView mRecyclerView;
    @Nullable
    @BindView(R2.id.notifications_empty_view) TextView mEmptyView;

    private NotificationsAdapter mAdapter;

    private void setupRecyclerView() {
        if (mRecyclerView != null) {
            mRecyclerView.addItemDecoration(
                    new DividerItemDecoration(mRecyclerView.getContext(), LinearLayoutManager.VERTICAL));

            mAdapter = new NotificationsAdapter(this, this);
            mRecyclerView.setAdapter(mAdapter);

            viewModel.getUserNotifications().observe(getViewLifecycleOwner(), mAdapter);
            viewModel.getUserNotifications().observe(getViewLifecycleOwner(), notifications -> {
                if (mRecyclerView != null) {
                    mRecyclerView.setVisibility(notifications.isEmpty() ? View.GONE : View.VISIBLE);
                }
                if (mEmptyView != null) {
                    mEmptyView.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    private void cleanupRecyclerView() {
        mAdapter = null;
    }
    // endregion RecyclerView

    private void loadFromRealm(boolean unReadOnly) {
        viewModel.getUnreadOnly().setValue(unReadOnly);
        if (unReadOnly) {
            mEventBus.post(new AnalyticsScreenEvent(SCREEN_NOTIFICATION_UNREAD));
        } else {
            mEventBus.post(new AnalyticsScreenEvent(SCREEN_NOTIFICATION_ALL));
        }
    }

    private void changeAccountList(String accountListId, String contactId) {
        appPrefs.setAccountListId(accountListId);
        mRealmManager.deleteRealm(false);

        ModalActivity.launchActivity(requireActivity(), UnlockFragment
                .create(DEEP_LINK_TYPE_CONTACTS, contactId, System.currentTimeMillis()));
        requireActivity().finish();
    }
}
