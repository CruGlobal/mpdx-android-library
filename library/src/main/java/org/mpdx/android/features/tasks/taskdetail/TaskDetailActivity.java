package org.mpdx.android.features.tasks.taskdetail;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.mpdx.android.R;
import org.mpdx.android.base.activity.BaseRealmActivity;
import org.mpdx.android.databinding.ActivityTaskDetailBinding;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.analytics.model.AnalyticsActionEvent;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.analytics.model.IconCallAnalyticsEvent;
import org.mpdx.android.features.analytics.model.IconEmailAnalyticsEvent;
import org.mpdx.android.features.analytics.model.IconTextAnalyticsEvent;
import org.mpdx.android.features.analytics.model.TaskCheckMarkClickedAction;
import org.mpdx.android.features.analytics.model.TaskDeletedAction;
import org.mpdx.android.features.contacts.ContactClickListener;
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivity;
import org.mpdx.android.features.contacts.model.Contact;
import org.mpdx.android.features.contacts.model.EmailAddress;
import org.mpdx.android.features.contacts.model.FacebookAccount;
import org.mpdx.android.features.contacts.model.Person;
import org.mpdx.android.features.contacts.model.PhoneNumber;
import org.mpdx.android.features.tasks.TaskModalUtility;
import org.mpdx.android.features.tasks.editor.TaskEditorActivityKt;
import org.mpdx.android.features.tasks.model.Task;
import org.mpdx.android.features.tasks.realm.TaskQueriesKt;
import org.mpdx.android.features.tasks.repository.TasksRepository;
import org.mpdx.android.features.tasks.tasklist.CurrentTasksFragment;
import org.mpdx.android.features.tasks.viewmodel.TaskViewModel;
import org.mpdx.android.utils.IntentUtilsKt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import dagger.hilt.android.AndroidEntryPoint;
import io.realm.RealmQuery;
import timber.log.Timber;

import static org.mpdx.android.features.analytics.model.AnalyticsActionEventKt.ACTION_TASK_DETAIL_ACTION;
import static org.mpdx.android.features.analytics.model.AnalyticsActionEventKt.CATEGORY_TASKS;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_TASK_DETAIL;
import static org.mpdx.android.features.contacts.contactdetail.ContactDetailActivity.CONTACT_ID_KEY;

@AndroidEntryPoint
@SuppressWarnings("HardCodedStringLiteral")
public class TaskDetailActivity extends BaseRealmActivity
        implements FacebookAccountClickedListener, ContactClickListener {
    private static final int REQUEST_FINISH_TASK = 1;

    public static final int TASK_DELETED_RESULT_CODE = 1;
    public static final int TASK_COMPLETED_RESULT_CODE = 2;
    public static final String DELETED_TASK_ID_KEY = "deleted_task_id_key";
    public static final String PENDING_ACTION_KEY = "pending_action_key";

    @Inject TasksRepository tasksRepository;
    @Inject AppPrefs appPrefs;

    private String taskId;
    private boolean isCompleted;
    private String pendingAction;

    // region Lifecycle
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseIntent();
        setupDataModel();

        setupBinding();
        setupToolbar();
        setupSwipeRefreshLayout();
        setupCommentsRecyclerView();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        switch (requestCode) {
            case REQUEST_FINISH_TASK:
                if (resultCode == RESULT_OK) {
                    setResult(TASK_COMPLETED_RESULT_CODE);
                    finish();
                }
                return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        cleanupCommentsRecyclerView();
        cleanupSwipeRefreshLayout();
        super.onDestroy();
    }
    // endregion Lifecycle

    public static Intent getIntent(Context context, String taskId, String activityType) {
        Intent intent = new Intent(context, TaskDetailActivity.class);
        intent.putExtra(CurrentTasksFragment.TASK_ID_KEY, taskId);
        if (activityType != null) {
            intent.putExtra(CurrentTasksFragment.TASK_ACTIVITY_TYPE_KEY, activityType);
        }
        return intent;
    }

    private void parseIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(CurrentTasksFragment.TASK_ID_KEY)) {
                taskId = intent.getStringExtra(CurrentTasksFragment.TASK_ID_KEY);
            }

            if (intent.hasExtra(CurrentTasksFragment.TASK_IS_COMPLETED)) {
                isCompleted = true;
            }

            if (intent.hasExtra(PENDING_ACTION_KEY)) {
                pendingAction = intent.getStringExtra(PENDING_ACTION_KEY);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.task_details_menu, menu);
        menu.findItem(R.id.task_details_delete).setVisible(isCompleted);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.task_details_delete) {
            listener.onTaskDelete(taskId);
            return true;
        } else if (itemId == R.id.task_details_edit) {
            if (!TextUtils.isEmpty(taskId)) {
                startActivity(TaskEditorActivityKt.buildTaskEditorActivityIntent(this, taskId));
            }
            return true;
        } else if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // region Data Model

    private TaskDetailActivityDataModel mDataModel;
    private TaskDetailActivityViewModel mViewModel;

    private void setupDataModel() {
        mDataModel = new ViewModelProvider(this).get(TaskDetailActivityDataModel.class);
        mDataModel.getTaskId().setValue(taskId);
        mDataModel.getTaskNotFound().observe(this, notFound -> {
            if (notFound) {
                // FIXME: disabled for now to prevent it being shown when deleting a task on mobile
//                Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_LONG).show();
                finish();
            }
        });

        mViewModel = new ViewModelProvider(this).get(TaskDetailActivityViewModel.class);
        mDataModel.getTask().observe(this, task -> mViewModel.getTask().setValue(task));
    }

    // endregion Data Model

    // region Data Binding
    private ActivityTaskDetailBinding binding;

    private void setupBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_task_detail);
        binding.setLifecycleOwner(this);
        binding.setContactClickListener(this);
        binding.setListener(listener);
        binding.setComments(mDataModel.getComments());
        binding.setContact(mDataModel.getContact());
        binding.setTask(new TaskViewModel());
        binding.setViewModel(mViewModel);

        mDataModel.getTask().observe(this, binding.getTask());
    }
    // endregion Data Binding

    // region Toolbar

    @Override
    protected void setupToolbar() {
        final Toolbar toolbar = binding.taskDetailToolbar;
        final Intent intent = getIntent();
        if (intent != null && !TextUtils.isEmpty(intent.getStringExtra(CurrentTasksFragment.TASK_ACTIVITY_TYPE_KEY))) {
            toolbar.setTitle(intent.getStringExtra(CurrentTasksFragment.TASK_ACTIVITY_TYPE_KEY).toLowerCase());
        }

        setSupportActionBar(toolbar);
        super.setupToolbar();
    }

    // endregion Toolbar

    // region SwipeRefreshLayout

    private Observer<Boolean> mSwipeRefreshLayoutObserver = state -> binding.refresh.setRefreshing(state);

    private void setupSwipeRefreshLayout() {
        mDataModel.getSyncTracker().isSyncing().observe(this, mSwipeRefreshLayoutObserver);
        binding.refresh.setOnRefreshListener(() -> mDataModel.syncData(true));
    }

    private void cleanupSwipeRefreshLayout() {
        mDataModel.getSyncTracker().isSyncing().removeObserver(mSwipeRefreshLayoutObserver);
        binding.refresh.setOnRefreshListener(null);
    }

    // endregion SwipeRefreshLayout

    // region Comments Recycler View
    private final CommentsAdapter commentsAdapter = new CommentsAdapter();

    private void setupCommentsRecyclerView() {
        final RecyclerView recycler = binding.comments;
        // XXX: I'm not sure if this is necessary or not -DF
        recycler.setNestedScrollingEnabled(false);
        recycler.setAdapter(commentsAdapter);
        final RecyclerView.ItemAnimator animator = recycler.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        mDataModel.getComments().observe(this, commentsAdapter);
    }

    private void cleanupCommentsRecyclerView() {
        binding.comments.setAdapter(null);
    }
    // endregion Comments Recycler View

    // region ContactClickListener
    @Override
    public void onContactClick(@Nullable final Contact contact) {
        onContactSelected(contact != null ? contact.getId() : null);
    }
    // endregion ContactClickListener

    public void onTaskDeleted(Task response) {
        if (response != null) {
            runOnUiThread(
                    () -> Toast.makeText(TaskDetailActivity.this, R.string.task_deleted, Toast.LENGTH_SHORT).show());

            Intent resultIntent = new Intent();
            final String type = response.getActivityType();
            if (type != null) {
                mEventBus.post(TaskDeletedAction.INSTANCE);
            }
            resultIntent.putExtra(DELETED_TASK_ID_KEY, taskId);
            setResult(TASK_DELETED_RESULT_CODE, resultIntent);
            finish();
        }
    }

    public void onTaskCompleted(@Nullable String taskId) {
        if (taskId == null) {
            return;
        }

        mEventBus.post(TaskCheckMarkClickedAction.INSTANCE);
        startActivityForResult(TaskEditorActivityKt.buildTaskEditorActivityIntent(this, taskId, false, true),
                               REQUEST_FINISH_TASK);
    }

    void onContactSelected(@Nullable final String contactId) {
        if (!TextUtils.isEmpty(contactId)) {
            Intent intent = new Intent(this, ContactDetailActivity.class);
            intent.putExtra(CONTACT_ID_KEY, contactId);
            intent.putExtra(CurrentTasksFragment.TASK_ID_KEY, taskId);
            intent.putExtra(CurrentTasksFragment.TASK_ACTIVITY_TYPE_KEY, mDataModel.getTask().getValue().getActivityType());
            startActivity(intent);
        }
    }

    public void onActionSelected(final Task task, AllowedActivityTypes activityType) {
        final List<PhoneNumber> numbers = task.getPhoneNumbers(false).findAll();
        if (numbers.isEmpty()) {
            Toast.makeText(this, R.string.no_phone_numbers, Toast.LENGTH_SHORT).show();
        } else {
            if (activityType == AllowedActivityTypes.TEXT_MESSAGE) {
                PhoneNumberTaskDialogFragment fragment = new PhoneNumberTaskDialogFragment(task.getId());
                fragment.show(getSupportFragmentManager(), fragment.getTag());
            } else {
                PhoneBottomSheetFragment fragment = PhoneBottomSheetFragment.newInstance(numbers, activityType);
                fragment.show(getSupportFragmentManager(), fragment.getTag());
            }
            mEventBus.post(new AnalyticsActionEvent(ACTION_TASK_DETAIL_ACTION, CATEGORY_TASKS,
                    activityType.toString()));
            appPrefs.setLastStartedAppId(taskId);
            appPrefs.setLastStartedApp(activityType.getApiValue());
        }
    }

    public void onEmailSelected(@Nullable final Task task) {
        final String taskId = task != null ? task.getId() : null;
        if (taskId == null) {
            return;
        }

        final RealmQuery<EmailAddress> query = task.getEmailAddresses(false);
        final List<EmailAddress> emails = query != null ? query.findAll() : Collections.emptyList();
        if (emails.isEmpty()) {
            Toast.makeText(this, R.string.no_email_addresses, Toast.LENGTH_SHORT).show();
        } else if (emails.size() == 1) {
            sendEmail(emails.get(0).getEmail());
        } else {
            new EmailTaskDialogFragment(taskId).show(getSupportFragmentManager(), "EmailBottomSheet");
        }
    }

    public void onFacebookSelected() {
        List<FacebookAccount> facebookAccounts = getFacebookAccounts();
        if (facebookAccounts.size() == 1) {
            openFacebookMessage(facebookAccounts.get(0));
        } else if (facebookAccounts.size() > 1) {
            new FacebookAccountSelectorDialogFragment(mDataModel.getContact().getValue().getId())
                    .show(getSupportFragmentManager(), "FacebookSelector");
        } else {
            Toast.makeText(this, R.string.no_facebook_accounts, Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private List<FacebookAccount> getFacebookAccounts() {
        List<FacebookAccount> facebookAccounts = new ArrayList<>();

        if (mDataModel.getContact().getValue() == null ||
                mDataModel.getContact().getValue().getPeople() == null) {
            return facebookAccounts;
        }

        for (Person person : mDataModel.getContact().getValue().getPeople().findAll()) {
            RealmQuery<FacebookAccount> personFaceBooks = person.getFacebookAccounts(false);
            if (personFaceBooks != null && personFaceBooks.findAll() != null) {
                facebookAccounts.addAll(personFaceBooks.findAll());
            }
        }

        return facebookAccounts;
    }

    private void openFacebookMessage(@Nullable final FacebookAccount account) {
        if (account == null) {
            return;
        }
        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(String.format("http://m.me/%s", account.getUsername())));
        startActivity(intent);
        appPrefs.setLastStartedAppId(taskId);
        appPrefs.setLastStartedApp(AllowedActivityTypes.FACEBOOK_MESSAGE.apiValue);
    }

    @Override
    public void onFacebookAccountClicked(@Nullable final FacebookAccount account) {
        openFacebookMessage(account);
    }

    void sendEmail(@NonNull final String... emails) {
        mEventBus.post(new AnalyticsActionEvent(ACTION_TASK_DETAIL_ACTION, CATEGORY_TASKS,
                AllowedActivityTypes.EMAIL.toString()));

        if (IntentUtilsKt.sendEmail(TaskDetailActivity.this, emails)) {

            // Log Task on Return
            appPrefs.setLastStartedAppId(taskId);
            appPrefs.setLastStartedApp(AllowedActivityTypes.EMAIL.apiValue);
        } else {
            Toast.makeText(this, R.string.no_email_client_found, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected String getPageName() {
        return "TaskDetail";
    }

    @Override
    protected void onResume() {
        super.onResume();

        Task realmTask = TaskQueriesKt.getTask(getRealm(), taskId).findFirst();
        final Contact firstContact = realmTask != null ? realmTask.getFirstContact() : null;
        final String contactId = firstContact != null ? firstContact.getId() : null;
        TaskModalUtility.showDialogIfPending(this, appPrefs, contactId, taskId);

        if (pendingAction != null) {
            if (pendingAction.equals(AllowedActivityTypes.CALL.apiValue) ||
                    pendingAction.equals(AllowedActivityTypes.TEXT_MESSAGE.apiValue)) {
                if (taskId != null && realmTask != null) {
                    onActionSelected(realmTask, AllowedActivityTypes.forApiValue(pendingAction));
                }
            } else if (pendingAction.equals(AllowedActivityTypes.EMAIL.apiValue)) {
                if (realmTask != null) {
                    onEmailSelected(realmTask);
                }
            } else if (pendingAction.equals(AllowedActivityTypes.FACEBOOK_MESSAGE)) {
                onFacebookSelected();
            }
            pendingAction = null;
        }

        mEventBus.post(new AnalyticsScreenEvent(SCREEN_TASK_DETAIL));
    }

    // region DetailActivity Listener
    TaskDetailListener listener = new TaskDetailListener() {

        @Override
        public void onTaskComplete(String id) {
            onTaskCompleted(id);
        }

        @Override
        public void onTaskDelete(String id) {
            tasksRepository.deleteTask(id);
            onTaskDeleted(mDataModel.getTask().getValue());
        }

        @Override
        public void onActionClicked() {
            Task task = mDataModel.getTask().getValue();
            String activityType = task.getActivityType();
            if (!TextUtils.isEmpty(activityType)) {
                switch (activityType) {
                    case "Call":
                        onActionSelected(task, AllowedActivityTypes.CALL);
                        mEventBus.post(IconCallAnalyticsEvent.INSTANCE);
                        break;
                    case "Text Message":
                        onActionSelected(task, AllowedActivityTypes.TEXT_MESSAGE);
                        mEventBus.post(IconTextAnalyticsEvent.INSTANCE);
                        break;
                    case "Email":
                        onEmailSelected(task);
                        mEventBus.post(IconEmailAnalyticsEvent.INSTANCE);
                        break;
                    case "Facebook Message":
                        onFacebookSelected();
                        break;
                    default:
                        // Do nothing
                        Timber.i("Unknown activity type clicked: %s", activityType);
                }
            }
        }
    };
    // endregion DetailActivity Listener
}
