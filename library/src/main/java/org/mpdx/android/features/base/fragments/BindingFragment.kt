package org.mpdx.android.features.base.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import butterknife.ButterKnife

abstract class BindingFragment<V : ViewDataBinding> : BaseFragment() {
    lateinit var binding: V
        private set

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, layoutRes(), container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        ButterKnife.bind(this, binding.root)
        return binding.root
    }
}
