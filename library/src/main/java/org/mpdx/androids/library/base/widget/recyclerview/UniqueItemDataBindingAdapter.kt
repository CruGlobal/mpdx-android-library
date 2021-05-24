package org.mpdx.androids.library.base.widget.recyclerview

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView.NO_ID
import org.ccci.gto.android.common.recyclerview.adapter.SimpleDataBindingAdapter
import org.ccci.gto.android.common.support.v4.util.IdUtils
import org.mpdx.androids.library.base.model.UniqueItem

abstract class UniqueItemDataBindingAdapter<T : UniqueItem, DB : ViewDataBinding>(
    lifecycleOwner: LifecycleOwner? = null
) : SimpleDataBindingAdapter<DB>(lifecycleOwner), Observer<List<T>> {
    var items: List<T>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = items?.size ?: 0
    override fun getItemId(position: Int) = items?.get(position)?.id?.let { IdUtils.convertId(it) } ?: NO_ID
    fun getItem(position: Int) = items?.get(position)

    override fun onChanged(t: List<T>?) {
        items = t
    }
}
