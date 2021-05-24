package org.mpdx.androids.library.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding

abstract class DataBindingFragment<DB : ViewDataBinding> : BaseFragment() {
    protected lateinit var binding: DB
        private set

    // region Lifecycle
    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = onCreateDataBinding(inflater, container)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    protected abstract fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?): DB
    // endregion Lifecycle
}
