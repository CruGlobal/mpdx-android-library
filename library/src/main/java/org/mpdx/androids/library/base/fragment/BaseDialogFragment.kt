package org.mpdx.androids.library.base.fragment

import android.content.Context
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

abstract class BaseDialogFragment : DialogFragment() {
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
