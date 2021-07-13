package org.mpdx.android.features.notifications.viewmodel

import org.mpdx.android.base.lifecycle.RealmModelViewModel
import org.mpdx.android.features.notifications.model.NotificationType

class NotificationTypeViewModel : RealmModelViewModel<NotificationType>() {
    // region Model Properties
    val description get() = model?.description
    // endregion Model Properties
}
