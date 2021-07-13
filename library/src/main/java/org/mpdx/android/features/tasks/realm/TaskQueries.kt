package org.mpdx.android.features.tasks.realm

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort
import io.realm.kotlin.where
import java.util.Calendar
import java.util.UUID
import org.mpdx.android.base.realm.between
import org.mpdx.android.base.realm.includeDeleted
import org.mpdx.android.base.realm.isDirty
import org.mpdx.android.core.model.AccountList
import org.mpdx.android.features.contacts.model.ContactFields
import org.mpdx.android.features.dashboard.connect.TaskActionTypeGrouping
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.model.TaskAnalytics
import org.mpdx.android.features.tasks.model.TaskAnalyticsFields
import org.mpdx.android.features.tasks.model.TaskFields
import org.mpdx.android.features.tasks.tasklist.TaskDueDateGrouping
import org.mpdx.android.utils.toDate
import org.mpdx.android.utils.toLocalDate
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

fun Realm.getTasks(accountListId: String?, includeDeleted: Boolean = false): RealmQuery<Task> = where<Task>()
    .equalTo(TaskFields.ACCOUNT_LIST.ID, accountListId)
    .includeDeleted(includeDeleted)

fun Realm.getTask(taskId: String? = null): RealmQuery<Task> = where<Task>().equalTo(TaskFields.ID, taskId)

fun Realm.getDirtyTasks() = where<Task>().isDirty().or().isDirty(TaskFields.TASK_CONTACTS.`$`)

fun RealmQuery<Task>.forContact(contactId: String?): RealmQuery<Task> =
    equalTo("${TaskFields.TASK_CONTACTS.CONTACT}.${ContactFields.ID}", contactId)

fun RealmQuery<Task>.isDue(dueDate: TaskDueDateGrouping): RealmQuery<Task> = when (dueDate) {
    TaskDueDateGrouping.OVERDUE -> isOverdue()
    TaskDueDateGrouping.TODAY -> isDueToday()
    TaskDueDateGrouping.OVERDUE_OR_TODAY -> isOverdueOrDueToday()
    TaskDueDateGrouping.TOMORROW -> isDueTomorrow()
    TaskDueDateGrouping.UPCOMING -> isUpcoming()
    TaskDueDateGrouping.NO_DUE_DATE -> hasNoDueDate()
    TaskDueDateGrouping.DUE_THIS_WEEK -> dueThisWeek()
}

fun RealmQuery<Task>.isOverdue(): RealmQuery<Task> = lessThan(TaskFields.START_AT, today.toDate())
fun RealmQuery<Task>.isDueToday(): RealmQuery<Task> = between(TaskFields.START_AT, today, tomorrow)
fun RealmQuery<Task>.isOverdueOrDueToday(): RealmQuery<Task> = lessThan(TaskFields.START_AT, tomorrow.toDate())
fun RealmQuery<Task>.isDueTomorrow(): RealmQuery<Task> = between(TaskFields.START_AT, tomorrow, dayAfterTomorrow)
fun RealmQuery<Task>.isUpcoming(): RealmQuery<Task> = greaterThan(TaskFields.START_AT, dayAfterTomorrow.toDate())
fun RealmQuery<Task>.hasNoDueDate(): RealmQuery<Task> = isNull(TaskFields.START_AT)
fun RealmQuery<Task>.dueThisWeek(): RealmQuery<Task> =
    between(TaskFields.START_AT, beginingOfWeek.toDate(), endOfWeek.toDate())

fun RealmQuery<Task>.hasType(type: TaskActionTypeGrouping): RealmQuery<Task> = when (type) {
    TaskActionTypeGrouping.NO_ACTION_SET -> isNull(TaskFields.ACTIVITY_TYPE)
    else -> equalTo(TaskFields.ACTIVITY_TYPE, type.apiLabel)
}

fun RealmQuery<Task>.completed(completed: Boolean): RealmQuery<Task> = equalTo(TaskFields.IS_COMPLETED, completed)

fun RealmQuery<Task>.sortByDate(sortOrder: Sort = Sort.ASCENDING): RealmQuery<Task> =
    sort(TaskFields.START_AT, sortOrder)

fun Realm.getTaskAnalytics(accountListId: String?): RealmQuery<TaskAnalytics> =
    where<TaskAnalytics>().equalTo(TaskAnalyticsFields.ACCOUNT_LIST_ID, accountListId)

private inline val today get() = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
private inline val tomorrow get() = today.plusDays(1)
private inline val dayAfterTomorrow get() = today.plusDays(2)
private inline val endOfWeek get() = LocalDate.now().with(DayOfWeek.SUNDAY)
private inline val beginingOfWeek: LocalDate
    get() {
        val calendar = Calendar.getInstance()
        calendar.time = endOfWeek.toDate()
        calendar.add(Calendar.DATE, -7)
        return calendar.time.toLocalDate()
    }

fun createTask(accountList: AccountList? = null) = Task().apply {
    id = UUID.randomUUID().toString()
    isNew = true
    this.accountList = accountList
}
