package org.mpdx.android.features.notifications.viewmodel

import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.features.notifications.model.UserNotification

class UserNotificationViewModel : ChangeAwareViewModel<UserNotification>() {
    override fun updateRelated(model: UserNotification?) {
        lazyNotification.model = model?.notification
    }

    // region Model Properties
    val isRead by modelBooleanProperty { it::isRead }
    // endregion Model Properties

    // region Related Models
    private val lazyNotification = LazyViewModel { NotificationViewModel() }
    val notification get() = lazyNotification.viewModel
    // endregion Related Models
}
