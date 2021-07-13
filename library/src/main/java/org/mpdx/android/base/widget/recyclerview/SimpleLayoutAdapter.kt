package org.mpdx.android.base.widget.recyclerview

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

class SimpleLayoutAdapter(@LayoutRes private val layout: Int) : RecyclerView.Adapter<SimpleLayoutViewHolder>() {
    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = 1
    override fun getItemId(position: Int) = 1L
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SimpleLayoutViewHolder(parent, layout)
    override fun onBindViewHolder(holder: SimpleLayoutViewHolder, position: Int) = Unit
}
