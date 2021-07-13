package org.mpdx.android.features.contacts.contactdetail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import org.mpdx.android.R;
import org.mpdx.android.core.modal.ModalActivity;
import org.mpdx.android.databinding.ActivityContactDetailBinding;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.base.BaseActivity;
import org.mpdx.android.features.contacts.ContactsPagerEnum;
import org.mpdx.android.features.contacts.ContactsPresenter;
import org.mpdx.android.features.contacts.contacteditor.ContactEditorActivityKt;
import org.mpdx.android.features.contacts.model.Contact;
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel;
import org.mpdx.android.features.donations.add.AddDonationFragment;
import org.mpdx.android.features.donations.list.DonationsFragment;
import org.mpdx.android.features.tasks.TaskModalUtility;
import org.mpdx.android.features.tasks.editor.TaskEditorActivityKt;
import org.mpdx.android.features.tasks.editor.TaskInitialProperties;
import org.mpdx.android.features.tasks.repository.TasksRepository;
import org.mpdx.android.features.tasks.tasklist.CurrentTasksFragment;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;

import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_CONTACTS_DONATIONS;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_CONTACTS_EDIT;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_CONTACTS_NOTES;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_CONTACTS_TASKS;

@AndroidEntryPoint
public class ContactDetailActivity extends BaseActivity {
    public static final String CONTACT_ID_KEY = "contact_id";
    public static final int REQUEST_TASK_HISTORY = 3;

    @Inject AppPrefs appPrefs;
    @Inject
    ContactsPresenter presenter;
    @Inject TasksRepository tasksRepository;

    private ActivityContactDetailBinding binding;
    private String contactId;
    private int currentView = 0;
    private String referringTaskId;
    private String referringTaskActivityType;
    private final ContactViewModel mContactViewModel = new ContactViewModel();
    private ContactDetailActivityViewModel viewModel;

    public static Intent getIntent(Context context, String contactId) {
        Intent i = new Intent(context, ContactDetailActivity.class);
        i.putExtra(CONTACT_ID_KEY, contactId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    // region Lifecycle Events

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityContactDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.setContact(mContactViewModel);
        binding.setPresenter(presenter);
        viewModel = new ViewModelProvider(this).get(ContactDetailActivityViewModel.class);

        setSupportActionBar(binding.contactsDetailToolbar);

        if (getSupportActionBar() != null) {
            setupToolbar();
        }

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(CONTACT_ID_KEY)) {
                contactId = intent.getStringExtra(CONTACT_ID_KEY);
            }
            if (intent.hasExtra(CurrentTasksFragment.TASK_ID_KEY)) {
                referringTaskId = intent.getStringExtra(CurrentTasksFragment.TASK_ID_KEY);
            }
            if (intent.hasExtra(CurrentTasksFragment.TASK_ACTIVITY_TYPE_KEY)) {
                referringTaskActivityType = intent.getStringExtra(CurrentTasksFragment.TASK_ACTIVITY_TYPE_KEY);
            }
        }

        initViewModel();
        setupSwipeRefreshLayout();
        setupViewPager();
        enableContactNotFoundCheck();

        binding.contactsTabLayout.setupWithViewPager(viewPager);
        currentView = 0;

        FabSpeedDial fabSpeedDial = binding.fabSpeedDialContact;
        fabSpeedDial.setMenuListener(new SimpleMenuListenerAdapter() {
            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_new_task) {
                    startActivity(TaskEditorActivityKt.buildTaskEditorActivityIntent(
                            ContactDetailActivity.this, null, false, false,
                            new TaskInitialProperties(null, null, new String[]{contactId})));
                } else if (itemId == R.id.action_log_task) {
                    startActivity(TaskEditorActivityKt.buildTaskEditorActivityIntent(
                            ContactDetailActivity.this, null, true, false,
                            new TaskInitialProperties(null, null, new String[]{contactId})));
                } else if (itemId == R.id.action_new_donation) {
                    AddDonationFragment addDonationFragment = AddDonationFragment.newInstance(
                            viewModel.getContact().getValue() != null ?
                                    viewModel.getContact().getValue() : new Contact());
                    ModalActivity.launchActivityForResult(ContactDetailActivity.this, addDonationFragment, DonationsFragment.REQUEST_ADD_DONATION);
                }
                return true;
            }
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        viewModel.getContact().observe(this, this::onContactUpdated);
        viewModel.getContact().observe(this, mContactViewModel);
    }

    @Override
    protected void onDestroy() {
        cleanupSwipeRefreshLayout();
        super.onDestroy();
    }

    // endregion Lifecycle Events

    private void initViewModel() {
        viewModel.getContactId().setValue(contactId);
    }

    // region SwipeRefreshLayout

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Observer<Boolean> mSwipeRefreshLayoutObserver = state -> mSwipeRefreshLayout.setRefreshing(state);

    private void setupSwipeRefreshLayout() {
        mSwipeRefreshLayout = binding.contactDetailSwipeRefreshLayout;
        viewModel.getSyncTracker().isSyncing().observe(this, mSwipeRefreshLayoutObserver);
        mSwipeRefreshLayout.setOnRefreshListener(() -> viewModel.syncData(true));
    }

    private void cleanupSwipeRefreshLayout() {
        viewModel.getSyncTracker().isSyncing().removeObserver(mSwipeRefreshLayoutObserver);
        mSwipeRefreshLayout.setOnRefreshListener(null);
    }

    // endregion SwipeRefreshLayout

    // region ViewPager

    private ViewPager viewPager;
    private ContactDetailFragmentPagerAdapter adapter;

    private void setupViewPager() {
        viewPager = binding.contactsViewpager;
        adapter = new ContactDetailFragmentPagerAdapter(this,
                                                referringTaskId, referringTaskActivityType);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(setViewPager);
    }

    // endregion ViewPager

    // region Contact Not Found Alert
    private boolean mContactNotFoundDialogShown = false;
    private final Observer<Boolean> mContactNotFoundObserver = notFound -> {
        if (notFound) {
            showContactNotFoundDialog();
        }
    };

    private void enableContactNotFoundCheck() {
        viewModel.getContactNotFound().observe(this, mContactNotFoundObserver);
    }

    private void showContactNotFoundDialog() {
        if (mContactNotFoundDialogShown) {
            return;
        }
        mContactNotFoundDialogShown = true;
        new AlertDialog.Builder(ContactDetailActivity.this).setMessage(R.string.contact_not_found)
                .setCancelable(false)
                .setNeutralButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }
    // endregion Contact Not Found Alert

    void onContactUpdated(Contact contact) {
        if (contact == null) {
            return;
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contacts_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.contacts_edit) {
            mEventBus.post(new AnalyticsScreenEvent(SCREEN_CONTACTS_EDIT));
            ContactEditorActivityKt.startContactEditorActivity(this, contactId);
            return true;
        } else if (itemId == R.id.menu_item_help) {
            launchHelpDialog();
            return true;
        } else if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    ViewPager.OnPageChangeListener setViewPager =

            new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    if (currentView == ContactsPagerEnum.NOTES.ordinal()) {
                        InputMethodManager inputMethodManager =
                                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(viewPager.getWindowToken(), 0);
                    }
                    currentView = position;

                    if (position == ContactsPagerEnum.NOTES.ordinal()) {
                        mEventBus.post(new AnalyticsScreenEvent(SCREEN_CONTACTS_NOTES));
                    } else if (position == ContactsPagerEnum.DONATION.ordinal()) {
                        mEventBus.post(new AnalyticsScreenEvent(SCREEN_CONTACTS_DONATIONS));
                    } else if (position == ContactsPagerEnum.TASKS.ordinal()) {
                        mEventBus.post(new AnalyticsScreenEvent(SCREEN_CONTACTS_TASKS));
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            };

    @Override
    protected void onResume() {
        super.onResume();
        TaskModalUtility.showDialogIfPending(this, appPrefs, contactId, null);
    }

    @Override
    protected String getPageName() {
        return "ContactDetails: " + ContactsPagerEnum.values()[currentView].name();
    }
}
