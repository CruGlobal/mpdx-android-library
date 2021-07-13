package org.mpdx.android.features.dashboard.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import org.mpdx.android.core.modal.ModalActivity;
import org.mpdx.android.features.dashboard.connect.TaskActionTypeGrouping;
import org.mpdx.android.features.tasks.model.OverdueTask;
import org.mpdx.android.features.tasks.tasklist.TaskDueDateGrouping;
import org.mpdx.android.features.tasks.tasklist.TasksViewAllFragment;

import java.util.List;

import androidx.annotation.Nullable;
import io.reactivex.functions.BiFunction;

public class ItemListView extends LinearLayout {

    private BiFunction<Object, ItemPairView, Void> bindingFunction;

    public ItemListView(Context context) {
        super(context);
        init();
    }

    public ItemListView(Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ItemListView(Context context,
            @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
    }

    public <T> void setBindFunction(BiFunction<T, ItemPairView, Void> bindingFunction) {
        this.bindingFunction = (BiFunction<Object, ItemPairView, Void>) bindingFunction;
    }

    public void bindData(List<OverdueTask> list) {
        removeAllViews();
        for (int i = 0; i < list.size(); i++) {
            ItemPairView itemPairView = new ItemPairView(getContext());
            final OverdueTask item = list.get(i);
            if (item != null && item.getCount() > 0) {
                itemPairView.setOnClickListener(view -> {
                    TasksViewAllFragment tasksViewAllFragment =
                            TasksViewAllFragment.newInstance(TaskActionTypeGrouping.fromCode(item.getLabel()),
                                                             TaskDueDateGrouping.OVERDUE_OR_TODAY);
                    ModalActivity.launchActivity((Activity) getContext(), tasksViewAllFragment);
                });
                try {
                    bindingFunction.apply(item, itemPairView);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                addView(itemPairView);
            }
        }
    }
}
