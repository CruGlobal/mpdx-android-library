package org.mpdx.android.features.tasks.tasklist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.core.data.repository.FilterRepository;
import org.mpdx.android.core.modal.ModalFragment;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.analytics.model.TaskCheckMarkClickedAction;
import org.mpdx.android.features.analytics.model.TaskDeletedAction;
import org.mpdx.android.features.base.fragments.BaseFragment;
import org.mpdx.android.features.dashboard.connect.TaskActionTypeGrouping;
import org.mpdx.android.features.tasks.editor.TaskEditorActivityKt;
import org.mpdx.android.features.tasks.model.Task;
import org.mpdx.android.features.tasks.repository.TasksRepository;
import org.mpdx.android.features.tasks.taskdetail.TaskDetailActivity;
import org.mpdx.android.utils.StringResolver;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import dagger.hilt.android.AndroidEntryPoint;
import io.huannguyen.swipeablerv.SWItemRemovalListener;
import io.huannguyen.swipeablerv.view.SWRecyclerView;

import static org.mpdx.android.features.tasks.taskdetail.TaskDetailActivity.TASK_COMPLETED_RESULT_CODE;
import static org.mpdx.android.features.tasks.taskdetail.TaskDetailActivity.TASK_DELETED_RESULT_CODE;
import static org.mpdx.android.features.tasks.tasklist.CurrentTasksFragment.TASK_ACTIVITY_TYPE_KEY;
import static org.mpdx.android.features.tasks.tasklist.CurrentTasksFragment.TASK_ID_KEY;

@AndroidEntryPoint
public class TasksViewAllFragment extends BaseFragment implements ModalFragment, TasksViewAllListener,
        SWItemRemovalListener<Task> {
    private static final String ARG_TYPE = "type";
    private static final String ARG_DUE_DATE = "due_date";

    private static final int TASK_DETAIL_REQUEST_CODE = 1;

    @Inject TasksRepository tasksRepository;
    @Inject FilterRepository filterRepository;
    @Inject StringResolver stringResolver;
    @Inject AppPrefs appPrefs;

    @BindView(R2.id.tasks_view_all_toolbar) Toolbar toolbar;
    @BindView(R2.id.tasks_view_all_recycler) SWRecyclerView recyclerView;
    @BindView(R2.id.task_count_text) TextView taskCountText;

    private TasksViewAllAdapter adapter;
    private TaskDueDateGrouping dueDateGrouping;
    private TaskActionTypeGrouping actionTypeGrouping;
    private String temporarilyDeletedId;
    private TasksViewAllFragmentViewModel viewModel;

    public static TasksViewAllFragment newInstance(TaskDueDateGrouping dueDate) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_DUE_DATE, dueDate);
        TasksViewAllFragment fragment = new TasksViewAllFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static TasksViewAllFragment newInstance(TaskActionTypeGrouping type, TaskDueDateGrouping dueDate) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_TYPE, type);
        bundle.putSerializable(ARG_DUE_DATE, dueDate);
        TasksViewAllFragment fragment = new TasksViewAllFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static TasksViewAllFragment newInstance() {
        return new TasksViewAllFragment();
    }

    // region Lifecycle Events

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        if (args != null) {
            dueDateGrouping = (TaskDueDateGrouping) args.getSerializable(ARG_DUE_DATE);
            actionTypeGrouping = (TaskActionTypeGrouping) args.getSerializable(ARG_TYPE);
        }

        viewModel = new ViewModelProvider(this).get(TasksViewAllFragmentViewModel.class);
        initViewModel();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new TasksViewAllAdapter(stringResolver, this);
        if (actionTypeGrouping != null) {
            toolbar.setTitle(getString(actionTypeGrouping.getStringResource()).toLowerCase());
        } else if (dueDateGrouping != null) {
            toolbar.setTitle(getString(dueDateGrouping.getStringResource()).toLowerCase());
        } else {
            toolbar.setTitle(getString(R.string.tasks_view_all));
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setupSwipeToDismiss(adapter, ItemTouchHelper.LEFT);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        adapter.setItemRemovalListener(this);
        recyclerView.setAdapter(adapter);

        retrieveAndFilterTasks();

        setupSwipeRefreshLayout();
    }

    @Override
    public void onDestroyView() {
        cleanupSwipeRefreshLayout();
        super.onDestroyView();
    }

    // endregion Lifecycle Events

    @Override
    protected int layoutRes() {
        return R.layout.fragment_tasks_view_all;
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    private void initViewModel() {
        viewModel.getTaskActionType().setValue(actionTypeGrouping);
        viewModel.getTaskDueDate().setValue(dueDateGrouping);
    }

    // region SwipeRefreshLayout
    @BindView(R2.id.all_tasks_swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private void setupSwipeRefreshLayout() {
        viewModel.getSyncTracker().isSyncing().observe(getViewLifecycleOwner(), state -> mSwipeRefreshLayout.setRefreshing(state));
        mSwipeRefreshLayout.setOnRefreshListener(() -> viewModel.syncData(true));
    }

    private void cleanupSwipeRefreshLayout() {
        mSwipeRefreshLayout.setOnRefreshListener(null);
    }
    // endregion SwipeRefreshLayout

    private void retrieveAndFilterTasks() {
        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            List<Task> filteredTasks = filterRepository.filterTasks(tasks);
            adapter.update(filteredTasks);
            taskCountText.setText(getString(R.string.task_count_text, filteredTasks.size()));
        });
    }

    @Override
    public void toTaskDetail(String taskId, String activityType) {
        Intent intent = new Intent(getContext(), TaskDetailActivity.class);
        intent.putExtra(TASK_ID_KEY, taskId);
        intent.putExtra(TASK_ACTIVITY_TYPE_KEY, activityType);
        startActivityForResult(intent, TASK_DETAIL_REQUEST_CODE);
    }

    @Override
    public void toViewAll(TaskDueDateGrouping grouping) {
        // Not implemented
    }

    @Override
    public void onTaskDeleted(Task response) {
        adapter.setState(AdapterState.DELETE);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(
                () -> Toast.makeText(activity, getString(R.string.task_deleted), Toast.LENGTH_SHORT).show());
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TASK_DETAIL_REQUEST_CODE) {
            if (resultCode == TASK_DELETED_RESULT_CODE) {
                adapter.setState(AdapterState.DELETE);
            } else if (resultCode == TASK_COMPLETED_RESULT_CODE) {
                adapter.setState(AdapterState.UPDATE);
            }
        }
    }

    @Override
    public void onItemTemporarilyRemoved(Task item, int position) {
        //Item was swiped away
        temporarilyDeletedId = item.getId();
    }

    @Override
    public void onStop() {
        if (temporarilyDeletedId != null) {
            tasksRepository.deleteTask(temporarilyDeletedId);
        }
        super.onStop();
    }

    @Override
    public void onItemPermanentlyRemoved(Task item) {
        if (!Objects.equals(item.getId(), temporarilyDeletedId)) return;
        onTaskDeleted(item);
        temporarilyDeletedId = null;
    }

    @Override
    public void onItemAddedBack(Task item, int position) {
        // Item was added back via snackbar undo
    }
}
