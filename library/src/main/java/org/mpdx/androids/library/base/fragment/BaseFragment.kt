package org.mpdx.androids.library.base.fragment

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

abstract class BaseFragment : Fragment() {
    protected val appCompatActivity get() = activity as? AppCompatActivity
    @Inject
    protected lateinit var eventBus: EventBus
    @Inject
    internal lateinit var lazyDaggerViewModelFactory: Lazy<ViewModelProvider.Factory>
    protected val daggerViewModelFactory: ViewModelProvider.Factory get() = lazyDaggerViewModelFactory.get()

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }
}
