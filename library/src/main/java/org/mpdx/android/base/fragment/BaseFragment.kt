package org.mpdx.android.base.fragment

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import javax.inject.Inject
import org.greenrobot.eventbus.EventBus

abstract class BaseFragment : Fragment() {
    protected val appCompatActivity get() = activity as? AppCompatActivity
    @Inject
    protected lateinit var eventBus: EventBus
}
