package org.mpdx.android.features.tasks.taskdetail

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.orEmpty
import org.ccci.gto.android.common.base.Constants.INVALID_DRAWABLE_RES
import org.mpdx.android.R
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.utils.StringResolver

@HiltViewModel
class TaskDetailActivityViewModel @Inject constructor(
    stringResolver: StringResolver
) : ViewModel() {
    val task = MutableLiveData<Task>()

    val activityActionText = task.map {
        for (activityTypes in AllowedActivityTypes.values()) {
            if (activityTypes.getApiValue() == it?.activityType) {
                return@map stringResolver.getString(activityTypes.getStringRes())
            }
        }
        return@map ""
    }

    val activityActionVisibility: LiveData<Int> = task.map {
        for (types in AllowedActivityTypes.values()) {
            if (types.getApiValue() == it?.activityType) {
                return@map View.VISIBLE
            }
        }
        return@map View.GONE
    }

    val taskContactsNames = task.switchMap { it?.getContacts(false)?.asLiveData().orEmpty() }
        .map { contacts -> contacts?.associateBy({ it }, { it.name }).orEmpty() }

    val activityActionIcon = task.map {
        return@map when (it?.activityType) {
            "Call" -> R.drawable.cru_icon_call_task
            "Text Message" -> R.drawable.cru_icon_text_message_task
            "Facebook Message" -> R.drawable.cru_icon_facebook_task
            "Email" -> R.drawable.cru_icon_email_task
            else -> INVALID_DRAWABLE_RES
        }
    }
}
