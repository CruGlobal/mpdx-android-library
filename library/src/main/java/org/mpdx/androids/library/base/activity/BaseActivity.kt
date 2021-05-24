package org.mpdx.androids.library.base.activity

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject

abstract class BaseActivity : org.mpdx.features.base.BaseActivity() {
    @Inject
    internal lateinit var lazyDaggerViewModelFactory: Lazy<ViewModelProvider.Factory>
    protected val daggerViewModelFactory: ViewModelProvider.Factory get() = lazyDaggerViewModelFactory.get()

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

    @JvmDefault
    fun onCreateDataBinding(binding: DB) = Unit
}
