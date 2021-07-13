package org.mpdx.android.core.domain;

import org.mpdx.android.base.model.UniqueItem;
import org.mpdx.android.features.tasks.tasklist.AdapterState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public abstract class UniqueItemAdapter<T extends UniqueItem, V extends RecyclerView.ViewHolder> extends
        RecyclerView.Adapter<V> implements UpdatableAdapter<T> {

    private AdapterState adapterState;
    protected List<T> items = new ArrayList<>();

    @Override
    public void update(List<T> list) {
        if (adapterState == AdapterState.UPDATE || adapterState == AdapterState.DELETE) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        } else if (items.size() > 0) {
            try {
                filter(items, list, UniqueItem::getId,
                        this::addAll);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            addAll(list);
        }
        adapterState = AdapterState.DEFAULT;
    }

    private void addAll(List<T> list) {
        int originalSize = items.size();
        items.addAll(list);
        if (originalSize == 0) {
            notifyDataSetChanged();
        } else {
            notifyItemRangeInserted(originalSize, list.size());
        }
    }

    public void setState(AdapterState state) {
        adapterState = state;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * See {@link UniqueItemAdapter#update(List)} for example
     *
     * @param original original list
     * @param update update batch, from I/O operation
     * @param getUniqueId An extractor for a unique id for an item.
     * @param listCallback callback for providing VALID diff between update & Original
     * @param <T> Type in list.
     * @throws Exception
     */
    public static <T> void filter(List<T> original, List<T> update, Function<T, String> getUniqueId,
            Consumer<List<T>> listCallback) throws Exception {
        List<T> res = new ArrayList<>();
        Map<String, T> map = new HashMap<>();

        for (int i = 0; i < original.size(); i++) {
            T value = original.get(i);
            map.put(getUniqueId.apply(value), value);
        }

        for (int i = 0; i < update.size(); i++) {
            T value = update.get(i);
            if (!map.containsKey(getUniqueId.apply(value))) {
                res.add(value);
            }
        }

        listCallback.accept(res);
    }
}
