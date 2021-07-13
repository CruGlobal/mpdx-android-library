package org.mpdx.android.features.tasks.tasklist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.mpdx.android.CurrentTaskHeaderItemBinding;
import org.mpdx.android.R;
import org.mpdx.android.core.domain.UniqueItemAdapter;
import org.mpdx.android.databinding.ItemTaskBinding;
import org.mpdx.android.features.tasks.model.Task;
import org.mpdx.android.features.tasks.tasklist.viewholders.CurrentTaskHeaderViewHolder;
import org.mpdx.android.features.tasks.tasklist.viewholders.CurrentTaskViewHolder;
import org.mpdx.android.utils.StringResolver;
import org.mpdx.android.utils.TasksComparator;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import androidx.recyclerview.widget.RecyclerView;
import io.huannguyen.swipeablerv.SWItemRemovalListener;
import io.huannguyen.swipeablerv.SWSnackBarDataProvider;
import io.huannguyen.swipeablerv.adapter.SWAdapter;
import io.huannguyen.swipeablerv.utils.ResourceUtils;

public class CurrentTasksAdapter extends UniqueItemAdapter<Task, RecyclerView.ViewHolder> implements
        SWAdapter<BaseViewItem> {
    private static final int HEADER_VIEW_TYPE = 0;
    private static final int TASK_VIEW_TYPE = 1;
    private static final int MAX_ITEMS_IN_GROUP = 3;

    private StringResolver stringResolver;
    private TasksComparator tasksComparator;
    private List<BaseViewItem> currentTaskAndHeaderItems = new ArrayList<>();
    private HashMap<String, Task> taskItemsMap = new HashMap<>();
    private SWItemRemovalListener<BaseViewItem> itemRemovalListener;
    private SWSnackBarDataProvider snackBarDataProvider;
    private CurrentTasksListener listener;

    @Inject
    public CurrentTasksAdapter(StringResolver stringResolver, TasksComparator tasksComparator, CurrentTasksListener listener) {
        this.stringResolver = stringResolver;
        this.tasksComparator = tasksComparator;
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case HEADER_VIEW_TYPE:
                CurrentTaskHeaderItemBinding currentTaskHeaderItemBinding =
                        CurrentTaskHeaderItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new CurrentTaskHeaderViewHolder(currentTaskHeaderItemBinding, stringResolver, listener);
            case TASK_VIEW_TYPE:
                ItemTaskBinding taskItemBinding =
                        ItemTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new CurrentTaskViewHolder(taskItemBinding, stringResolver, listener);
            default:
                return null;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return currentTaskAndHeaderItems.get(position).getViewType();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case HEADER_VIEW_TYPE:
                HeaderViewItem headerViewItem = (HeaderViewItem) currentTaskAndHeaderItems.get(position);
                ((CurrentTaskHeaderViewHolder) holder).update(headerViewItem.getGrouping());
                break;
            case TASK_VIEW_TYPE:
                CurrentTaskViewItem currentTaskViewItem = (CurrentTaskViewItem) currentTaskAndHeaderItems.get(position);
                if (currentTaskViewItem.getCurrentTask().isValid()) {
                    ((CurrentTaskViewHolder) holder).update(currentTaskViewItem.getCurrentTask());
                }
                break;
            default:
                break;
        }
    }

    public void completeTask(Task task) {
        for (int i = 0; i < currentTaskAndHeaderItems.size(); i++) {
            String id = currentTaskAndHeaderItems.get(i).getId();
            if (id != null && id.equals(task.getId())) {
                currentTaskAndHeaderItems.remove(i);
                taskItemsMap.remove(id);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return currentTaskAndHeaderItems.size();
    }

    public void clear() {
        currentTaskAndHeaderItems.clear();
        taskItemsMap.clear();
    }

    public void update(List<Task> currentTasks, TaskDueDateGrouping grouping) {
        if (currentTasks.size() == 0) {
            return;
        }

        HeaderViewItem headerViewItem = new HeaderViewItem(grouping);
        int insertPos = currentTaskAndHeaderItems.indexOf(headerViewItem);
        if (insertPos < 0) {
            currentTaskAndHeaderItems.add(new HeaderViewItem(grouping));
        } else {
            insertPos++;
        }

        int itemsAddedToGroup = 0;
        for (Task task : currentTasks) {
            if (itemsAddedToGroup < MAX_ITEMS_IN_GROUP && !taskItemsMap.containsKey(task.getId())) {
                if (insertPos > 0) {
                    currentTaskAndHeaderItems.add(insertPos, new CurrentTaskViewItem(task));
                } else {
                    currentTaskAndHeaderItems.add(new CurrentTaskViewItem(task));
                }
                itemsAddedToGroup++;
                taskItemsMap.put(task.getId(), task);
            } else {
                break;
            }
        }
    }

    public void update(List<Task> currentTasks) {
        Map<TaskDueDateGrouping, List<Task>> groupings = groupAndFilterByDueDate(currentTasks);
        currentTaskAndHeaderItems.clear();
        taskItemsMap.clear();

        for (TaskDueDateGrouping dueDateGrouping : groupings.keySet()) {
            List<Task> list = groupings.get(dueDateGrouping);
            Collections.sort(list, tasksComparator);
            groupings.put(dueDateGrouping, list);

            HeaderViewItem headerViewItem = new HeaderViewItem(dueDateGrouping);
            if (!currentTaskAndHeaderItems.contains(headerViewItem)) {
                currentTaskAndHeaderItems.add(new HeaderViewItem(dueDateGrouping));
            }

            int itemsAddedToGroup = 0;
            for (Task task : groupings.get(dueDateGrouping)) {
                if (!taskItemsMap.containsKey(task.getId()) && itemsAddedToGroup < MAX_ITEMS_IN_GROUP) {
                    currentTaskAndHeaderItems.add(new CurrentTaskViewItem(task));
                    itemsAddedToGroup++;
                }
                taskItemsMap.put(task.getId(), task);
            }
        }
    }

    @Override
    public void setSnackBarDataProvider(SWSnackBarDataProvider snackBarDataProvider) {
        this.snackBarDataProvider = snackBarDataProvider;
    }

    @Override
    public void onItemCleared(RecyclerView.ViewHolder viewHolder, int direction) {
        int adapterPosition = viewHolder.getAdapterPosition();
        CurrentTaskViewItem item = (CurrentTaskViewItem) currentTaskAndHeaderItems.get(adapterPosition);

        displayTaskDeletedSnackBar(viewHolder, item, adapterPosition, direction);

        if (itemRemovalListener != null) {
            itemRemovalListener.onItemTemporarilyRemoved(item, adapterPosition);
        }
        currentTaskAndHeaderItems.remove(adapterPosition);
        notifyItemRemoved(adapterPosition);
    }

    @Override
    public String getSnackBarMessage(RecyclerView.ViewHolder viewHolder, int direction) {
        if (snackBarDataProvider != null) {
            return snackBarDataProvider.getSnackBarMessage(direction);
        }

        return null;
    }

    @Override
    public String getUndoActionText(RecyclerView.ViewHolder viewHolder, int direction) {
        if (snackBarDataProvider != null) {
            return snackBarDataProvider.getUndoActionText(direction);
        }

        return null;
    }

    @Override
    public int getSwipeDirs(RecyclerView.ViewHolder viewHolder) {
        // Don't handle swipes on Header items
        if (viewHolder.getItemViewType() == HEADER_VIEW_TYPE) {
            return 0;
        }

        return -1;
    }

    @Override
    public SWItemRemovalListener getItemRemovalListener() {
        return itemRemovalListener;
    }

    @Override
    public void setItemRemovalListener(SWItemRemovalListener<BaseViewItem> itemRemovalListener) {
        this.itemRemovalListener = itemRemovalListener;
    }

    private void displayTaskDeletedSnackBar(RecyclerView.ViewHolder viewHolder, BaseViewItem item, int adapterPosition,
            int direction) {
        if (snackBarDataProvider != null && snackBarDataProvider.isUndoEnabled()) {
            final Snackbar snackbar = Snackbar
                    .make(snackBarDataProvider.getView(), getSnackBarMessage(viewHolder, direction),
                            Snackbar.LENGTH_LONG)
                    .setAction(getUndoActionText(viewHolder, direction),
                            v -> {
                                currentTaskAndHeaderItems.add(adapterPosition, item);
                                notifyItemInserted(adapterPosition);
                                if (itemRemovalListener != null) {
                                    itemRemovalListener.onItemAddedBack(item, adapterPosition);
                                }
                            });

            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    if (event != DISMISS_EVENT_ACTION && itemRemovalListener != null) {
                        itemRemovalListener.onItemPermanentlyRemoved(item);
                    }
                }
            });

            // Set colors
            View snackBarView = snackbar.getView();
            int snackBarBackgroundColor = snackBarDataProvider.getSnackBarBackgroundColor(direction);
            if (snackBarBackgroundColor != ResourceUtils.NO_COLOR) {
                snackBarView.setBackgroundColor(snackBarBackgroundColor);
            }

            int undoActionTextColor = snackBarDataProvider.getUndoActionTextColor(direction);
            if (undoActionTextColor != ResourceUtils.NO_COLOR) {
                snackbar.setActionTextColor(undoActionTextColor);
            }

            int infoMessageColor = snackBarDataProvider.getSnackBarMessageColor(direction);
            if (infoMessageColor != ResourceUtils.NO_COLOR) {
                TextView textView = snackBarView.findViewById(R.id.snackbar_text);
                textView.setTextColor(infoMessageColor);
            }

            snackbar.show();
            snackbar.getView().setOnClickListener(v -> snackbar.dismiss());
        }
    }

    private Map<TaskDueDateGrouping, List<Task>> groupAndFilterByDueDate(
            List<Task> tasks) {
        Map<TaskDueDateGrouping, List<Task>> map = new TreeMap<>();
        for (Task currentTask : tasks) {
            if (currentTask.getStartAt() == null) {
                if (map.containsKey(TaskDueDateGrouping.NO_DUE_DATE)) {
                    map.get(TaskDueDateGrouping.NO_DUE_DATE).add(currentTask);
                } else {
                    List<Task> noDueDateItems = new ArrayList<>();
                    noDueDateItems.add(currentTask);
                    map.put(TaskDueDateGrouping.NO_DUE_DATE, noDueDateItems);
                }
            } else {
                LocalDate taskDate = LocalDate.from(Instant.ofEpochMilli(currentTask.getStartAt().getTime()).atZone(
                        ZoneId.systemDefault()));
                switch (TaskDueDateGrouping.convert(taskDate)) {
                    case OVERDUE:
                        if (map.containsKey(TaskDueDateGrouping.OVERDUE)) {
                            map.get(TaskDueDateGrouping.OVERDUE).add(currentTask);
                        } else {
                            List<Task> overdueItems = new ArrayList<>();
                            overdueItems.add(currentTask);
                            map.put(TaskDueDateGrouping.OVERDUE, overdueItems);
                        }
                        break;
                    case TODAY:
                        if (map.containsKey(TaskDueDateGrouping.TODAY)) {
                            map.get(TaskDueDateGrouping.TODAY).add(currentTask);
                        } else {
                            List<Task> todayItems = new ArrayList<>();
                            todayItems.add(currentTask);
                            map.put(TaskDueDateGrouping.TODAY, todayItems);
                        }
                        break;
                    case TOMORROW:
                        if (map.containsKey(TaskDueDateGrouping.TOMORROW)) {
                            map.get(TaskDueDateGrouping.TOMORROW).add(currentTask);
                        } else {
                            List<Task> tomorrowItems = new ArrayList<>();
                            tomorrowItems.add(currentTask);
                            map.put(TaskDueDateGrouping.TOMORROW, tomorrowItems);
                        }
                        break;
                    default:
                        if (map.containsKey(TaskDueDateGrouping.UPCOMING)) {
                            map.get(TaskDueDateGrouping.UPCOMING).add(currentTask);
                        } else {
                            List<Task> upcomingItems = new ArrayList<>();
                            upcomingItems.add(currentTask);
                            map.put(TaskDueDateGrouping.UPCOMING, upcomingItems);
                        }
                }
            }
        }

        return map;
    }

    public class HeaderViewItem extends BaseViewItem {

        private TaskDueDateGrouping grouping;

        HeaderViewItem(TaskDueDateGrouping grouping) {
            this.grouping = grouping;
        }

        public TaskDueDateGrouping getGrouping() {
            return grouping;
        }

        @Override
        public int getViewType() {
            return HEADER_VIEW_TYPE;
        }

        @Override
        public String getId() {
            return "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HeaderViewItem that = (HeaderViewItem) o;
            return grouping == that.grouping;
        }

        @Override
        public int hashCode() {
            return grouping.hashCode();
        }
    }

    public class CurrentTaskViewItem extends BaseViewItem {

        private Task model;

        CurrentTaskViewItem(Task model) {
            this.model = model;
        }

        Task getCurrentTask() {
            return model;
        }

        @Override
        public int getViewType() {
            return TASK_VIEW_TYPE;
        }

        @Override
        public String getId() {
            return model.getId();
        }
    }

}
