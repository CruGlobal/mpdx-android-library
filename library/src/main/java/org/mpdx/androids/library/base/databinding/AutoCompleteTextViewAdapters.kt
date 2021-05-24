package org.mpdx.androids.library.base.databinding

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.annotation.LayoutRes
import androidx.databinding.BindingAdapter
import org.mpdx.R

@BindingAdapter("dropDownItems", "dropDownItemLayout", "dropDownItemsIncludeEmpty", requireAll = false)
fun AutoCompleteTextView.setItems(items: Array<String>?, @LayoutRes layout: Int?, includeEmpty: Boolean?) =
    setItems(items?.toList(), layout, includeEmpty)

@BindingAdapter("dropDownItems", "dropDownItemLayout", "dropDownItemsIncludeEmpty", requireAll = false)
fun AutoCompleteTextView.setItems(items: List<String>?, @LayoutRes layout: Int?, includeEmpty: Boolean?) {
    val list = when {
        includeEmpty != true -> items.orEmpty()
        items == null -> listOf("")
        else -> listOf("") + items
    }
    setAdapter(ArrayAdapter(context, layout ?: R.layout.mtrl_dropdown_menu_popup_item, list))
}
