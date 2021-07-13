package org.mpdx.android.base.fragment

import android.content.Context
import androidx.fragment.app.DialogFragment
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import org.greenrobot.eventbus.EventBus

abstract class BaseDialogFragment : DialogFragment() {
    @Inject
    protected lateinit var eventBus: EventBus

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }
}
