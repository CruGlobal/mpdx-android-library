package org.mpdx.android.features.notifications.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.lifecycle.LifecycleOwner
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.databinding.ItemNotificationBinding
import org.mpdx.android.features.notifications.model.UserNotification
import org.mpdx.android.features.notifications.viewmodel.UserNotificationViewModel

class NotificationsAdapter internal constructor(
    lifecycleOwner: LifecycleOwner? = null,
    listener: OnNotificationSelectedListener? = null
) : UniqueItemRealmDataBindingAdapter<UserNotification, ItemNotificationBinding>(lifecycleOwner) {
    val listener = ObservableField<OnNotificationSelectedListener>()
        .apply { set(listener) }

    // region Lifecycle Events
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            notificationSelectedListener = listener
            notification = UserNotificationViewModel()
        }

    override fun onBindViewDataBinding(binding: ItemNotificationBinding, position: Int) {
        binding.notification?.model = getItem(position)
    }
    // endregion Lifecycle Events
}
