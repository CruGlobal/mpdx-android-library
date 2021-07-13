package org.mpdx.android.features.coaching;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.core.model.AccountList;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.base.BaseActivity;
import org.mpdx.android.features.coaching.model.CoachingAnalytics;
import org.mpdx.android.features.coaching.model.CoachingAppointmentResults;
import org.mpdx.android.features.coaching.model.stats.CoachingStat;
import org.mpdx.android.features.coaching.model.stats.CoachingStatWithCurrency;
import org.mpdx.android.features.tasks.model.TaskAnalytics;
import org.mpdx.android.utils.DateUtils;
import org.mpdx.android.utils.DateUtilsKt;
import org.threeten.bp.LocalDate;
import org.threeten.bp.YearMonth;
import org.threeten.bp.temporal.ChronoUnit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import dagger.hilt.android.AndroidEntryPoint;
import io.realm.Realm;
import kotlin.ranges.ClosedRange;

import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_COACHING_DETAIL_MONTH;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_COACHING_DETAIL_WEEK;
import static org.mpdx.android.utils.DateUtilsKt.toContainingWeekRange;

@AndroidEntryPoint
public class CoachingDetailActivity extends BaseActivity {
    private static final int MONTH_TAB_INDEX = 0;
    private static final String UTC_CODE = "UTC";

    public static final String EXTRA_ACCOUNT_ID = "accountId";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private CoachingView coachingView;
    private String accountId;
    private AccountList account;
    private RecyclerView periodRecycler;
    private LinearLayout statList;
    private TextViewHolder selectedView;
    private int selectedPosition = 0;

    public static Intent getIntent(Context context, String accountId) {
        Intent i = new Intent(context, CoachingDetailActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(EXTRA_ACCOUNT_ID, accountId);
        return i;
    }

    // region Lifecycle

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountId = getIntent().getStringExtra(EXTRA_ACCOUNT_ID);

        toolbar = findViewById(R.id.coaching_detail_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            setupToolbar();
        }

        setupDataModel();
        setupSwipeRefreshLayout();

        coachingView = findViewById(R.id.coaching_header_view);

        tabLayout = findViewById(R.id.coaching_period_tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.coaching_tab_month));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.coaching_tab_week));

        periodRecycler = findViewById(R.id.coaching_period_recycler);
        periodRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true));
        periodRecycler.setAdapter(new RecyclerView.Adapter<TextViewHolder>() {
            private SimpleDateFormat monthFormatter = new SimpleDateFormat("MMMM");
            private SimpleDateFormat weekFormatter = new SimpleDateFormat("MMM d");

            @NonNull
            @Override
            public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView textView = new TextView(CoachingDetailActivity.this);
                textView.setLayoutParams(new ViewGroup.LayoutParams(
                        getResources().getDimensionPixelOffset(R.dimen.coaching_date_selector_width),
                        getResources().getDimensionPixelOffset((R.dimen.coaching_date_selector_height))));
                textView.setGravity(Gravity.CENTER);
                final TextViewHolder viewHolder = new TextViewHolder(textView);
                viewHolder.setListener(position -> {
                    if (selectedView != null) {
                        selectedView.deselect();
                    } else if (selectedPosition == 0 && position != 0) {
                        selectedPosition = position;
                        periodRecycler.getAdapter().notifyItemChanged(0);
                    }
                    selectedView = viewHolder;
                    selectedPosition = position;

                    loadData(tabLayout.getSelectedTabPosition(), selectedPosition);
                });

                return viewHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {
                if (tabLayout.getSelectedTabPosition() == MONTH_TAB_INDEX) {
                    holder.update(position, monthFormatter.format(DateUtils.iterateBack(position).getTime()));
                } else {
                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(UTC_CODE));
                    calendar.add(Calendar.WEEK_OF_YEAR, -position);
                    calendar.add(Calendar.DAY_OF_WEEK, -(calendar.get(Calendar.DAY_OF_WEEK) - 1));
                    String firstDay = weekFormatter.format(calendar.getTime());
                    calendar.add(Calendar.DAY_OF_WEEK, 6);
                    String lastDay = weekFormatter.format(calendar.getTime());
                    holder.update(position, firstDay + " - " + lastDay);
                }
            }

            @Override
            public int getItemCount() {
                return 2500; // 50ish years.
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                periodRecycler.getAdapter().notifyDataSetChanged();
                selectedPosition = 0;
                selectedView = null;

                loadData(tab.getPosition(), selectedPosition);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }
            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        statList = findViewById(R.id.coaching_stat_list);
        TextView newsletterText = findViewById(R.id.coaching_last_newsletter_text);
        newsletterText.setText(getDaysSinceLastNewsletterText());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccount();
        loadData(tabLayout.getSelectedTabPosition(), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupSwipeRefreshLayout();
    }

    // endregion Lifecycle

    // region Data Model
    private CoachingDetailActivityDataModel mDataModel;

    private void setupDataModel() {
        mDataModel = new ViewModelProvider(this).get(CoachingDetailActivityDataModel.class);
        mDataModel.getAccountListId().setValue(accountId);
        mDataModel.getAnalytics().observe(this, analytics -> updateStatListView());
        mDataModel.getAppointments().observe(this, this::onAppointmentsLoaded);
    }

    // endregion Data Model

    // region SwipeRefresh

    @BindView(R2.id.refresh)
    SwipeRefreshLayout mSwipeRefreshLayout;
    private Observer<Boolean> mSwipeRefreshLayoutObserver = state -> mSwipeRefreshLayout.setRefreshing(state);

    private void setupSwipeRefreshLayout() {
        mDataModel.getSyncTracker().isSyncing().observe(this, mSwipeRefreshLayoutObserver);
        mSwipeRefreshLayout.setOnRefreshListener(() -> mDataModel.syncData(true));
    }

    private void cleanupSwipeRefreshLayout() {
        mDataModel.getSyncTracker().isSyncing().removeObserver(mSwipeRefreshLayoutObserver);
        mSwipeRefreshLayout.setOnRefreshListener(null);
    }

    // endregion SwipeRefresh

    private void loadAccount() {
        mDataModel.getAccountList().observe(this, accountList -> {
            if (accountList == null) {
                return;
            }
            account = accountList;
            coachingView.setAccountList(account);
            toolbar.setTitle(account.getName());
            coachingView.setTitle(R.string.coaching_account_balance);
        });
    }

    private void loadData(int tabPosition, int position) {
        if (tabPosition == MONTH_TAB_INDEX) {
            mEventBus.post(new AnalyticsScreenEvent(SCREEN_COACHING_DETAIL_MONTH));
            mDataModel.getRange().setValue(createMonthDateRange(position));
        } else {
            mEventBus.post(new AnalyticsScreenEvent(SCREEN_COACHING_DETAIL_WEEK));
            mDataModel.getRange().setValue(createWeekDateRange(position));
        }
    }

    @Nullable
    private CoachingAppointmentResults mAppointmentResults = null;

    private void onAppointmentsLoaded(@Nullable final CoachingAppointmentResults results) {
        mAppointmentResults = results;
        updateStatListView();
    }

    private void updateStatListView() {
        statList.removeAllViews();
        final CoachingAnalytics analytics = mDataModel.getAnalytics().getValue();
        CoachingStat[] stats = analytics != null ? analytics.getStats() : new CoachingStat[0];
        for (CoachingStat stat : stats) {
            if (stat != null) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        getResources().getDimensionPixelOffset(R.dimen.coaching_stat_row_height));
                statList.addView(new CoachingStatRow(this, stat), params);
            }
        }
        if (mAppointmentResults != null) {
            for (CoachingStatWithCurrency stat : mAppointmentResults.getStats()) {
                if (stat != null) {
                    String currency = null;
                    if (account != null && account.getCurrency() != null) {
                        currency = account.getCurrency();
                    }
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            getResources().getDimensionPixelOffset(R.dimen.coaching_stat_row_height));
                    statList.addView(new CoachingStatRow(this, stat, currency), params);
                }
            }
        }
    }

    @Override
    public int layoutId() {
        return R.layout.activity_coaching_detail;
    }

    @Override
    protected String getPageName() {
        if (account == null) {
            return "";
        }
        return account.getName();
    }

    private ClosedRange<LocalDate> createMonthDateRange(int position) {
        return DateUtilsKt.toLocalDateRange(YearMonth.now().minusMonths(position));
    }

    private ClosedRange<LocalDate> createWeekDateRange(int position) {
        // HACK: we force the US locale to have weeks always start on Sunday per an Ellis requirement for the US
        //       coaching week to start on Sunday.
        return DateUtilsKt.minusWeeks(toContainingWeekRange(LocalDate.now(), Locale.US), position);
    }

    private class TextViewHolder extends RecyclerView.ViewHolder {
        private int position;
        private TextView view;

        TextViewHolder(View view) {
            super(view);
            this.view = (TextView) view;
        }

        void setListener(ItemSelectedListener listener) {
            view.setOnClickListener(v -> {
                if (selectedView == this) {
                    return;
                }
                select();
                listener.itemSelected(position);
            });
        }

        void update(int index, String text) {
            position = index;
            view.setText(text);
            if (selectedPosition == index) {
                select();
            } else {
                deselect();
            }
        }

        void select() {
            view.setBackgroundResource(R.drawable.bottom_border_yellow);
            view.setTextColor(ContextCompat.getColor(CoachingDetailActivity.this, (R.color.primary_blue)));
        }

        void deselect() {
            view.setBackground(null);
            view.setTextColor(ContextCompat.getColor(CoachingDetailActivity.this, (R.color.black)));
        }
    }

    private interface ItemSelectedListener {
        void itemSelected(int position);
    }

    private Spanned getDaysSinceLastNewsletterText() {
        Realm realm = Realm.getDefaultInstance();
        TaskAnalytics taskAnalytics = realm.where(TaskAnalytics.class).findFirst();
        long days = 0;

        if (taskAnalytics != null) {
            Date lastElectronicNewsletter = taskAnalytics.getLastElectronicNewsletterCompletedAt();
            Date lastPhysicalNewsletter = taskAnalytics.getLastPhysicalNewsletterCompletedAt();
            LocalDate now = LocalDate.now();
            // Subtract a day to account for 24 hour overlap
            if (lastPhysicalNewsletter == null && lastElectronicNewsletter == null) {
                days = -1;
            } else if (lastPhysicalNewsletter != null && lastElectronicNewsletter == null) {
                days = ChronoUnit.DAYS.between(DateUtils.toLocalDate(lastPhysicalNewsletter), now) - 1;
            } else if (lastPhysicalNewsletter == null && lastElectronicNewsletter != null) {
                days = ChronoUnit.DAYS.between(DateUtils.toLocalDate(lastElectronicNewsletter), now) - 1;
            } else {
                if (lastPhysicalNewsletter.after(lastElectronicNewsletter)) {
                    days = ChronoUnit.DAYS.between(DateUtils.toLocalDate(lastPhysicalNewsletter), now) - 1;
                } else {
                    days = ChronoUnit.DAYS.between(DateUtils.toLocalDate(lastElectronicNewsletter), now) - 1;
                }
            }
        }
        realm.close();

        if (days == -1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Html.fromHtml(getString(R.string.days_since_last_newsletter_never), Html.FROM_HTML_MODE_COMPACT);
            } else {
                return Html.fromHtml(getString(R.string.days_since_last_newsletter_never));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Html.fromHtml(getString(R.string.days_since_last_newsletter_html,
                        String.valueOf(days)), Html.FROM_HTML_MODE_COMPACT);
            } else {
                return Html.fromHtml(getString(R.string.days_since_last_newsletter_html,
                        String.valueOf(days)));
            }
        }
    }
}
