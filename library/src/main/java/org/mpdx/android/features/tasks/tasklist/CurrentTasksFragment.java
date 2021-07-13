package org.mpdx.android.features.tasks.tasklist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.core.data.repository.FilterRepository;
import org.mpdx.android.core.modal.ModalActivity;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.analytics.model.TaskCheckMarkClickedAction;
import org.mpdx.android.features.analytics.model.TaskDeletedAction;
import org.mpdx.android.features.base.fragments.BaseFragment;
import org.mpdx.android.features.tasks.editor.TaskEditorActivityKt;
import org.mpdx.android.features.tasks.model.Task;
import org.mpdx.android.features.tasks.realm.TaskQueriesKt;
import org.mpdx.android.features.tasks.repository.TasksRepository;
import org.mpdx.android.features.tasks.taskdetail.TaskDetailActivity;
import org.mpdx.android.utils.StringResolver;
import org.mpdx.android.utils.TasksComparator;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import dagger.hilt.android.AndroidEntryPoint;
import io.huannguyen.swipeablerv.SWItemRemovalListener;
import io.huannguyen.swipeablerv.view.SWRecyclerView;
import io.realm.Realm;

import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_TASKS_CURRENT;

@AndroidEntryPoint
public class CurrentTasksFragment extends BaseFragment
        implements CurrentTasksListener, SWItemRemovalListener<BaseViewItem> {

    public static final String TASK_ID_KEY = "task_id";
    public static final String TASK_ACTIVITY_TYPE_KEY = "task_activity_type";
    public static final String TASK_IS_COMPLETED = "task_is_completed";
    private static final int TASK_LIST_REQUEST_CODE = 2;

    @Inject TasksRepository tasksRepository;
    @Inject FilterRepository filterRepository;
    @Inject StringResolver stringResolver;
    @Inject TasksComparator tasksComparator;
    private String temporarilyDeletedId;

    @BindView(R2.id.current_tasks_recycler) SWRecyclerView recyclerView;

    private CurrentTasksAdapter adapter;
    private CurrentTasksFragmentViewModel viewModel;

    public CurrentTasksFragment() {

    }

    public static CurrentTasksFragment newInstance(String taskId, long time) {
        CurrentTasksFragment fragment = new CurrentTasksFragment();
        fragment.setDeepLinkId(taskId, time);
        return fragment;
    }

    // region Lifecycle Events

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CurrentTasksFragmentViewModel.class);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CurrentTasksAdapter(stringResolver, tasksComparator, this);
        recyclerView.setupSwipeToDismiss(adapter, ItemTouchHelper.LEFT);
        adapter.setItemRemovalListener(this);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);

        if (getArguments() != null && getArguments().getString(ARG_DEEP_LINK_ID) != null) {
            String deepLinkId = getArguments().getString(ARG_DEEP_LINK_ID);
            toTaskDetail(deepLinkId, null);
            clearDeepLink();
        }

        setupSwipeRefreshLayout();

        retrieveAndFilterTasks();
    }

    @Override
    public void onResume() {
        super.onResume();
        mEventBus.post(new AnalyticsScreenEvent(SCREEN_TASKS_CURRENT));
    }

    @Override
    public void onDestroyView() {
        cleanupSwipeRefreshLayout();
        super.onDestroyView();
    }

    // endregion Lifecycle Events

    @Override
    protected int layoutRes() {
        return R.layout.fragment_current_tasks;
    }

    // region SwipeRefreshLayout
    @BindView(R2.id.current_tasks_swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private void setupSwipeRefreshLayout() {
        viewModel.getSyncTracker().isSyncing()
                .observe(getViewLifecycleOwner(), state -> mSwipeRefreshLayout.setRefreshing(state));
        mSwipeRefreshLayout.setOnRefreshListener(() -> viewModel.syncData(true));
    }

    private void cleanupSwipeRefreshLayout() {
        mSwipeRefreshLayout.setOnRefreshListener(null);
    }
    // endregion SwipeRefreshLayout

    private void retrieveAndFilterTasks() {
        viewModel.getOverdueTasks().observe(getViewLifecycleOwner(), tasks -> updateAdapter());
        viewModel.getTodayTasks().observe(getViewLifecycleOwner(), tasks -> updateAdapter());
        viewModel.getTomorrowTasks().observe(getViewLifecycleOwner(), tasks -> updateAdapter());
        viewModel.getUpcomingTasks().observe(getViewLifecycleOwner(), tasks -> updateAdapter());
        viewModel.getNoDueDateTasks().observe(getViewLifecycleOwner(), tasks -> updateAdapter());
    }

    private void updateAdapter() {
        adapter.clear();
        adapter.update(filterRepository.filterTasks(viewModel.getOverdueTasks().getValue(), 3),
                       TaskDueDateGrouping.OVERDUE);
        adapter.update(filterRepository.filterTasks(viewModel.getTodayTasks().getValue(), 3),
                       TaskDueDateGrouping.TODAY);
        adapter.update(filterRepository.filterTasks(viewModel.getTomorrowTasks().getValue(), 3),
                       TaskDueDateGrouping.TOMORROW);
        adapter.update(filterRepository.filterTasks(viewModel.getUpcomingTasks().getValue(), 3),
                       TaskDueDateGrouping.UPCOMING);
        adapter.update(filterRepository.filterTasks(viewModel.getNoDueDateTasks().getValue(), 3),
                       TaskDueDateGrouping.NO_DUE_DATE);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void toTaskDetail(String taskId, String activityType) {
        Intent intent = new Intent(getContext(), TaskDetailActivity.class);
        intent.putExtra(TASK_ID_KEY, taskId);
        intent.putExtra(TASK_ACTIVITY_TYPE_KEY, activityType);
        startActivityForResult(intent, TASK_LIST_REQUEST_CODE);
    }

    @Override
    public void toViewAll(TaskDueDateGrouping grouping) {
        TasksViewAllFragment tasksViewAllFragment = TasksViewAllFragment.newInstance(grouping);
        ModalActivity.launchActivity(getActivity(), tasksViewAllFragment);
    }

    @Override
    public void onTaskDeleted(Task response) {
        adapter.setState(AdapterState.DELETE);
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> Toast.makeText(activity, R.string.task_deleted, Toast.LENGTH_SHORT).show());
        }

        if (response != null && response.getId() != null) {
            mEventBus.post(TaskDeletedAction.INSTANCE);
        }
    }

    @Override
    public void onTaskCompleted(String taskId) {
        if (taskId == null) {
            return;
        }

        mEventBus.post(TaskCheckMarkClickedAction.INSTANCE);
        startActivity(TaskEditorActivityKt.buildTaskEditorActivityIntent(requireContext(), taskId, false, true));
    }

    @Override
    public void onItemTemporarilyRemoved(BaseViewItem item, int position) {
        //Item was swiped away
        temporarilyDeletedId = ((CurrentTasksAdapter.CurrentTaskViewItem) item).getCurrentTask().getId();
    }

    @Override
    public void onStop() {
        if (temporarilyDeletedId != null) {
            tasksRepository.deleteTask(temporarilyDeletedId);
        }
        super.onStop();
    }

    @Override
    public void onItemAddedBack(BaseViewItem item, int position) {
        // Item was added back via snackbar undo
    }

    @Override
    public void onItemPermanentlyRemoved(BaseViewItem item) {
        // Item was swiped away and action can no longer be undone
        if (!item.getId().equals(temporarilyDeletedId)) return;
        Realm realm = Realm.getDefaultInstance();
        onTaskDeleted(TaskQueriesKt.getTask(realm, temporarilyDeletedId).findFirst());
        realm.close();
        temporarilyDeletedId = null;
    }
}
