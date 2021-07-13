package org.mpdx.android.core.coreutils;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;

import org.mpdx.android.features.dashboard.view.ItemListView;
import org.mpdx.android.features.tasks.model.OverdueTask;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;

public class GeneralBindingAdapters {
    @BindingAdapter("android:src")
    public static void setImageViewResource(ImageView imageView, int resource) {
        imageView.setImageResource(resource);
    }

    @BindingAdapter(value = {"bindItemList", "bindItemLimit"}, requireAll = false)
    public static void bindItemList(ItemListView itemListView, List<OverdueTask> list, Integer limit) {
        if (list == null) {
            list = new ArrayList<>();
        }
        if (limit != null && limit < list.size()) {
            list = list.subList(0, limit);
        }
        itemListView.bindData(list);
    }

    @BindingAdapter("android:drawableLeft")
    public static void setDrawableLeft(TextView textView, int drawableRes) {
        if (drawableRes != 0) {
            Drawable drawable = ContextCompat.getDrawable(textView.getContext(), drawableRes);
            Drawable[] existingDrawables = textView.getCompoundDrawables();
            textView.setCompoundDrawablesWithIntrinsicBounds(drawable, existingDrawables[1], existingDrawables[2],
                    existingDrawables[3]);
        }
    }

    @BindingAdapter("android:drawableTop")
    public static void setDrawableTop(TextView textView, int drawableRes) {
        if (drawableRes != 0) {
            Drawable drawable = ContextCompat.getDrawable(textView.getContext(), drawableRes);
            Drawable[] existingDrawables = textView.getCompoundDrawables();
            textView.setCompoundDrawablesWithIntrinsicBounds(existingDrawables[0], drawable, existingDrawables[2],
                    existingDrawables[3]);
        }
    }
}
