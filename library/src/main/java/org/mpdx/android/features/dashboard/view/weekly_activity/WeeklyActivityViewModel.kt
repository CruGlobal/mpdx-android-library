package org.mpdx.android.features.dashboard.view.weekly_activity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Case
import io.realm.kotlin.oneOf
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.combineWith
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.tasks.model.TaskFields
import org.mpdx.android.features.tasks.realm.getTasks
import org.mpdx.android.utils.toDate
import org.mpdx.android.utils.toLocalDate
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.format.TextStyle

@HiltViewModel
class WeeklyActivityViewModel @Inject constructor(appPrefs: AppPrefs) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData

    // region Date
    private val endOfWeekLocalDate = MutableLiveData(LocalDate.now().with(DayOfWeek.SUNDAY))

    private val beginningOfWeekLocalDate = endOfWeekLocalDate.map {
        val calendar = Calendar.getInstance()
        calendar.time = it.toDate()
        calendar.add(Calendar.DATE, -7)
        return@map calendar.time.toLocalDate()
    }

    val weekDateText = endOfWeekLocalDate.combineWith(beginningOfWeekLocalDate) { end, begin ->
        val endOfWeekMonthDisplay = end.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val beginningWeekMonthDisplay = begin.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        if (end.monthValue != begin.monthValue) {
            return@combineWith "$beginningWeekMonthDisplay ${begin.dayOfMonth} ${end.year} - " +
                "$endOfWeekMonthDisplay ${end.dayOfMonth} ${end.year} "
        }
        return@combineWith "$endOfWeekMonthDisplay ${begin.dayOfMonth} - " +
            "${end.dayOfMonth} ${end.year}"
    }
    // endregion Date

    // region Calls
    private val callCompleted =
        accountListId.switchCombineWith(beginningOfWeekLocalDate, endOfWeekLocalDate) { id, begin, end ->
            realm.getTasks(id)
                .contains(TaskFields.ACTIVITY_TYPE, "Call")
                .between(TaskFields.COMPLETED_AT_VALUE, begin.toDate(), end.toDate())
                .asLiveData()
        }

    private val callsProduced =
        accountListId.switchCombineWith(beginningOfWeekLocalDate, endOfWeekLocalDate) { id, begin, end ->
            realm.getTasks(id)
                .contains(TaskFields.ACTIVITY_TYPE, "Call")
                .contains(TaskFields.NEXT_ACTION, "Appointment")
                .between(TaskFields.COMPLETED_AT_VALUE, begin.toDate(), end.toDate())

                .asLiveData()
        }

    val callCompletedCount = callCompleted.map { "${it.size}" }
    val callProducedCount = callsProduced.map { "${it.size}" }
    // endregion Calls

    private fun messageQuery(accountId: String?) = realm.getTasks(accountId)
        .oneOf(TaskFields.ACTIVITY_TYPE, arrayOf("Email", "Facebook Message", "Text Message"), Case.INSENSITIVE)

    // region Message
    private val messagesCompleted =
        accountListId.switchCombineWith(beginningOfWeekLocalDate, endOfWeekLocalDate) { id, begin, end ->
            messageQuery(id)
                .between(TaskFields.COMPLETED_AT_VALUE, begin.toDate(), end.toDate())
                .asLiveData()
        }

    private val messagesCreated =
        accountListId.switchCombineWith(beginningOfWeekLocalDate, endOfWeekLocalDate) { id, begin, end ->
            messageQuery(id)
                .contains(TaskFields.NEXT_ACTION, "Appointment")
                .between(TaskFields.COMPLETED_AT_VALUE, begin.toDate(), end.toDate())
                .asLiveData()
        }

    val messagesCompletedCount = messagesCompleted.map { "${it.size}" }
    val messagesProducedCount = messagesCreated.map { "${it.size}" }
    //endregion Message

    // region Appointment
    private val appointmentsCompleted =
        accountListId.switchCombineWith(beginningOfWeekLocalDate, endOfWeekLocalDate) { id, begin, end ->
            realm.getTasks(id).contains(TaskFields.ACTIVITY_TYPE, "Appointment")
                .between(TaskFields.COMPLETED_AT_VALUE, begin.toDate(), end.toDate())
                .asLiveData()
        }

    val appointmentsCompletedCount = appointmentsCompleted.map { "${it.size}" }
    // endregion Appointment

    // region Correspondence
    private fun getCorrespondenceQuery(accountListId: String?) = realm.getTasks(accountListId)
        .beginGroup()
        .contains(TaskFields.ACTIVITY_TYPE, "Letter", Case.INSENSITIVE)
        .or()
        .contains(TaskFields.ACTIVITY_TYPE, "Thank", Case.INSENSITIVE)
        .endGroup()

    private val correspondenceCompleted =
        accountListId.switchCombineWith(beginningOfWeekLocalDate, endOfWeekLocalDate) { id, begin, end ->
            getCorrespondenceQuery(id)
                .between(TaskFields.COMPLETED_AT_VALUE, begin.toDate(), end.toDate())
                .asLiveData()
        }

    private val correspondenceCreated =
        accountListId.switchCombineWith(beginningOfWeekLocalDate, endOfWeekLocalDate) { id, begin, end ->
            getCorrespondenceQuery(id)
                .contains(TaskFields.NEXT_ACTION, "Appointment")
                .between(TaskFields.COMPLETED_AT_VALUE, begin.toDate(), end.toDate())
                .asLiveData()
        }

    val correspondenceCompletedCount = correspondenceCompleted.map { "${it.size}" }
    // endregion Correspondence
}
