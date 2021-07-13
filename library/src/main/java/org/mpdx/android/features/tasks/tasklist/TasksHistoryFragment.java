package org.mpdx.android.features.tasks.tasklist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.base.widget.recyclerview.FilteredEndlessScrollListener;
import org.mpdx.android.core.data.repository.FilterRepository;
import org.mpdx.android.core.modal.ModalActivity;
import org.mpdx.android.core.modal.ModalFragment;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.base.fragments.BaseFragment;
import org.mpdx.android.features.tasks.BaseTaskListener;
import org.mpdx.android.features.tasks.model.Task;
import org.mpdx.android.features.tasks.sync.TasksSyncService;
import org.mpdx.android.features.tasks.taskdetail.TaskDetailActivity;
import org.mpdx.android.utils.StringResolver;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import dagger.hilt.android.AndroidEntryPoint;

import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_TASKS_HISTORY;
import static org.mpdx.android.features.tasks.tasklist.CurrentTasksFragment.TASK_ACTIVITY_TYPE_KEY;
import static org.mpdx.android.features.tasks.tasklist.CurrentTasksFragment.TASK_IS_COMPLETED;

@AndroidEntryPoint
@SuppressWarnings("HardCodedStringLiteral")
public class TasksHistoryFragment extends BaseFragment implements ModalFragment, BaseTaskListener {

    public static final String TASK_ID_KEY = "task_id";
    private static final String CONTACT_ID_KEY = "contact_id";
    private static final int TASK_LIST_REQUEST_CODE = 2;

    @Inject AppPrefs appPrefs;
    @Inject StringResolver stringResolver;
    @Inject FilterRepository filterRepository;

    @BindView(R2.id.tasks_view_all_toolbar) Toolbar toolbar;

    @Nullable
    private String contactId;
    private TasksHistoryFragmentViewModel viewModel;

    public TasksHistoryFragment() {
    }

    public static TasksHistoryFragment newInstance() {
        return new TasksHistoryFragment();
    }

    public static TasksHistoryFragment newInstance(String contactId) {

        Bundle args = new Bundle();
        args.putString(CONTACT_ID_KEY, contactId);

        TasksHistoryFragment fragment = new TasksHistoryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // region Lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TasksHistoryFragmentViewModel.class);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!showToolbar()) {
            toolbar.setVisibility(View.GONE);
        }

        if (isContactTaskHistory()) {
            contactId = getArguments().getString(CONTACT_ID_KEY);
        }

        initViewModel();
        setupSwipeRefreshLayout();
        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        mEventBus.post(new AnalyticsScreenEvent(SCREEN_TASKS_HISTORY));
    }

    @Override
    public void onDestroyView() {
        cleanupRecyclerView();
        cleanupSwipeRefreshLayout();
        super.onDestroyView();
    }
    // endregion Lifecycle

    private void initViewModel() {
        viewModel.getContactId().setValue(contactId);
    }

    @Override
    protected int layoutRes() {
        return R.layout.fragment_task_history;
    }

    // region RecyclerView
    @BindView(R2.id.tasks_empty_view)
    TextView mNoTasksView;
    @BindView(R2.id.tasks_history_recycler)
    RecyclerView mRecyclerView;
    private TasksHistoryAdapter mAdapter;
    private final FilteredEndlessScrollListener mEndlessScrollListener =
            new FilteredEndlessScrollListener(TasksSyncService.PAGE_SIZE,
                                              (page, force) -> viewModel.syncPageOfTasks(page, force));

    private void setupRecyclerView() {
        mAdapter = new TasksHistoryAdapter(this, stringResolver);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        if (!isContactTaskHistory()) {
            mRecyclerView.addOnScrollListener(mEndlessScrollListener);
        }

        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            List<Task> filteredTasks = filterRepository.filterTasks(tasks);
            mAdapter.setItems(filteredTasks);
            if (filteredTasks.size() == 0) {
                mNoTasksView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            } else {
                mNoTasksView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }

            if (!isContactTaskHistory()) {
                mEndlessScrollListener.setFilteredItems(filteredTasks.size());
                mEndlessScrollListener.setUnfilteredItems(tasks != null ? tasks.size() : 0);
            }
        });
    }

    private void cleanupRecyclerView() {
        mRecyclerView.removeOnScrollListener(mEndlessScrollListener);
    }
    // endregion RecyclerView

    // region SwipeRefreshLayout
    @BindView(R2.id.tasks_refresh)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private void setupSwipeRefreshLayout() {
        viewModel.getSyncTracker().isSyncing()
                .observe(getViewLifecycleOwner(), state -> mSwipeRefreshLayout.setRefreshing(state));
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (isContactTaskHistory()) {
                viewModel.syncContactTasks(true);
            } else {
                mEndlessScrollListener.setForce(true);
            }
        });
    }

    private void cleanupSwipeRefreshLayout() {
        mSwipeRefreshLayout.setOnRefreshListener(null);
    }
    // endregion SwipeRefreshLayout

    @Override
    public void toTaskDetail(String taskId, String activityType) {
        Intent intent = new Intent(getContext(), TaskDetailActivity.class);
        intent.putExtra(TASK_ID_KEY, taskId);
        intent.putExtra(TASK_ACTIVITY_TYPE_KEY, activityType);
        intent.putExtra(TASK_IS_COMPLETED, true);

        startActivityForResult(intent, TASK_LIST_REQUEST_CODE);
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    private boolean showToolbar() {
        return getActivity() instanceof ModalActivity;
    }

    private boolean isContactTaskHistory() {
        return getArguments() != null && getArguments().containsKey(CONTACT_ID_KEY);
    }

    @Override
    public void onTaskCompleted(@Nullable String taskId) { }
}
