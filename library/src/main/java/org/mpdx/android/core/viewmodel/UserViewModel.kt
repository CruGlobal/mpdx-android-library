package org.mpdx.android.core.viewmodel

import org.mpdx.android.base.lifecycle.RealmModelViewModel
import org.mpdx.android.core.model.User

class UserViewModel : RealmModelViewModel<User>() {
    override fun createModel() = User()

    // region Generated Properties
    val fullName get() = listOfNotNull(model?.firstName, model?.lastName).joinToString(" ")
    // endregion Generated Properties
}
