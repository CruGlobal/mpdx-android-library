package org.mpdx.android.base.widget.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

class SimpleLayoutViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
