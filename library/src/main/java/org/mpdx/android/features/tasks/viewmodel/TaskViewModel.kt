package org.mpdx.android.features.tasks.viewmodel

import androidx.databinding.Bindable
import androidx.lifecycle.map
import org.ccci.gto.android.common.androidx.lifecycle.databinding.getPropertyLiveData
import org.mpdx.android.BR
import org.mpdx.android.R
import org.mpdx.android.base.databinding.ValidationHelper
import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.base.model.addTag
import org.mpdx.android.base.model.removeTag
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.features.constants.Constants
import org.mpdx.android.features.contacts.viewmodel.ContactViewModel
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.utils.toZonedDateTime
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

class TaskViewModel : ChangeAwareViewModel<Task>() {
    override fun createModel() = Task()
    override fun updateRelated(model: Task?) {
        contacts.managed = model?.getContacts()?.asLiveData()
    }

    // region Model Properties
    @get:Bindable
    var subject by modelStringProperty(BR.subject) { it::subject }
    @get:Bindable
    var type by modelStringProperty(
        BR.type,
        onSet = {
            if (!Constants.getResults(it).contains(result?.value)) result = null
            if (!Constants.getNextActionsValues(it).contains(nextAction)) nextAction = null
        }
    ) { it::activityType }
    @get:Bindable
    var dueDate by modelNullableProperty(BR.dueDate) { it::dueDate }
    @get:Bindable
    var completedAt by modelNullableProperty(BR.completedAt) { it::completedAt }
    @get:Bindable
    var notificationType by modelProperty(
        BR.notificationType,
        Task.NotificationType.DEFAULT,
        onSet = {
            if (it != Task.NotificationType.NONE) notificationTimeUnit = notificationTimeUnit ?: ChronoUnit.MINUTES
        }
    ) { it::notificationType }
    @get:Bindable
    var notificationTime by modelProperty(
        BR.notificationTime,
        0,
        onSet = {
            if (it > 0) notificationTimeUnit = notificationTimeUnit ?: ChronoUnit.MINUTES
        }
    ) { it::notificationTimeBefore }
    @get:Bindable
    var notificationTimeUnit by modelNullableProperty(BR.notificationTimeUnit) { it::notificationTimeUnit }
    @get:Bindable
    var nextAction by modelNullableProperty(BR.nextAction) { it::nextAction }
    @get:Bindable
    var result by modelNullableProperty(BR.result) { it::result }

    val isSubjectHidden get() = model?.isSubjectHidden ?: false
    var isCompleted by modelBooleanProperty(valueIfNull = false) { it::isCompleted }
    // endregion Model Properties

    // region Transformed Properties
    @get:Bindable("type")
    var typeLabel
        get() = Constants.getTaskTypeLabel(type)
        set(value) {
            type = Constants.getTaskTypeValue(value)
        }

    @get:Bindable("dueDate")
    var dueDateDate
        get() = dueDate?.toLocalDate()
        set(value) {
            dueDate = value?.toZonedDateTime(dueDateTime)
        }
    @get:Bindable("dueDate")
    var dueDateTime
        get() = dueDate?.toLocalTime()
        set(value) {
            dueDate = dueDateDate?.toZonedDateTime(value)
        }

    @get:Bindable("completedAt")
    var completedAtDate
        get() = completedAt?.toLocalDate()
        set(value) {
            completedAt = value?.toZonedDateTime(completedAtTime)
        }
    @get:Bindable("completedAt")
    var completedAtTime
        get() = completedAt?.toLocalTime()
        set(value) {
            completedAt = completedAtDate?.toZonedDateTime(value)
        }

    @get:Bindable("nextAction")
    var nextActionLabel
        get() = Constants.getTaskTypeLabel(nextAction)
        set(value) {
            nextAction = Constants.getTaskTypeValue(value)
        }
    // endregion Transformed Properties

    // region Contacts
    @get:Bindable
    val contacts = LazyRelatedModels(BR.contacts) {
        ContactViewModel().also {
            it.allowNullModel = allowNullModel
            it.forceUnmanaged = forceUnmanaged
            it.trackingChanges = false
        }
    }

    val contactIdsLiveData by lazy {
        contacts.initialize()
        getPropertyLiveData(::contacts, BR.contacts).map { it.ids }
    }
    // endregion Contacts

    // region Tags
    @get:Bindable
    val tags get() = model?.tags.orEmpty()
    val tagsLiveData by lazy { getPropertyLiveData(::tags, BR.tags) }

    fun addTag(tag: String) {
        if (model?.addTag(tag) == true) notifyPropertyChanged(BR.tags)
    }

    fun deleteTag(tag: String) {
        if (model?.removeTag(tag) == true) notifyPropertyChanged(BR.tags)
    }
    // endregion Tags

    // region Validation
    override fun hasErrors() = completedAtValidation.hasError
    override fun showErrors() {
        completedAtValidation.showErrors = true
    }

    @get:Bindable("completedAt")
    val completedAtValidation = ValidationHelper {
        when {
            model?.isCompleted == true && model?.completedAt == null -> R.string.task_without_date_and_time
            model?.isCompleted == true && model?.completedAt?.isAfter(ZonedDateTime.now()) == true ->
                R.string.log_task_in_future_error
            else -> null
        }
    }
    // endregion Validation
}
