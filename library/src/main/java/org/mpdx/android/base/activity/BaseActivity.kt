package org.mpdx.android.base.activity

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class BaseActivity : org.mpdx.android.features.base.BaseActivity() {

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        dataBindingActivity?.createDataBinding()
    }
    // endregion Lifecycle

    // region Data Binding
    private inline val dataBindingActivity get() = this as? DataBindingActivity<*>

    private fun <DB : ViewDataBinding> DataBindingActivity<DB>.createDataBinding() {
        binding = DataBindingUtil.inflate(layoutInflater, layoutId(), null, false)
        binding.lifecycleOwner = this@BaseActivity
        onCreateDataBinding(binding)
        setContentView(binding.root)
    }
    // endregion Data Binding

    override fun setupUI() {
        if (dataBindingActivity == null) super.setupUI()
    }
}

interface DataBindingActivity<DB : ViewDataBinding> {
    var binding: DB

    fun onCreateDataBinding(binding: DB) = Unit
}
