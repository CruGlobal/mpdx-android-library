package org.mpdx.android.base.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import org.ccci.gto.android.common.androidx.lifecycle.widget.ObserverArrayAdapter

abstract class DataBindingObserverArrayAdapter<B : ViewDataBinding, T>(
    context: Context,
    private val layout: Int,
    private val lifecycleOwner: LifecycleOwner? = null
) : ObserverArrayAdapter<T>(context, layout) {
    protected abstract fun onBindingCreated(binding: B)
    protected abstract fun onBind(binding: B, position: Int)

    final override fun getView(position: Int, convertView: View?, parent: ViewGroup) =
        (convertView?.getBinding() ?: inflateBinding(parent))
            .also { onBind(it, position) }
            .root

    private fun View.getBinding(): B? = DataBindingUtil.getBinding(this)
    private fun inflateBinding(parent: ViewGroup): B =
        DataBindingUtil.inflate<B>(LayoutInflater.from(parent.context), layout, parent, false)
            .also {
                it.lifecycleOwner = lifecycleOwner
                onBindingCreated(it)
            }
}
