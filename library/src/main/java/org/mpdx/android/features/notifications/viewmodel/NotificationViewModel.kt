package org.mpdx.android.features.notifications.viewmodel

import org.mpdx.android.base.lifecycle.RealmModelViewModel
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel
import org.mpdx.android.features.notifications.model.Notification
import org.mpdx.android.utils.toLocalDate

class NotificationViewModel : RealmModelViewModel<Notification>() {
    override fun updateRelated(model: Notification?) {
        lazyContact.model = model?.contact
        lazyType.model = model?.notificationType
    }

    // region Related Models
    private val lazyContact = LazyViewModel { ContactViewModel() }
    val contact get() = lazyContact.viewModel

    private val lazyType = LazyViewModel { NotificationTypeViewModel() }
    val type get() = lazyType.viewModel
    // endregion Related Models

    // region Transformed Properties
    val date get() = model?.eventDate?.toLocalDate()
    // endregion Transformed Properties
}
