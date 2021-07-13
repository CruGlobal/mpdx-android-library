package org.mpdx.android.features.dashboard.view.to_due_this_week

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.combineWith
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.constants.model.ConstantList.Companion.STATUS_PARTNER_FINANCIAL
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.realm.anniversaryBetween
import org.mpdx.android.features.contacts.realm.birthdayBetween
import org.mpdx.android.features.contacts.realm.donationLateState
import org.mpdx.android.features.contacts.realm.forAccountList
import org.mpdx.android.features.contacts.realm.getContacts
import org.mpdx.android.features.contacts.realm.getPeople
import org.mpdx.android.features.contacts.realm.status
import org.mpdx.android.features.tasks.model.TaskFields
import org.mpdx.android.features.tasks.realm.completed
import org.mpdx.android.features.tasks.realm.getTasks
import org.mpdx.android.utils.toDate
import org.mpdx.android.utils.toLocalDate
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.MonthDay

const val DASHBOARD_BIRTHDAYS = "birthday_dashboard"
const val DASHBOARD_ANNIVERSARY = "anniversary_dashboard"

@HiltViewModel
class DashboardDueThisWeekViewModel @Inject constructor(appPrefs: AppPrefs) : RealmViewModel() {
    private val accountListId = appPrefs.accountListIdLiveData

    // region Date
    private val endOfWeekLocalDate = MutableLiveData(LocalDate.now().with(DayOfWeek.SUNDAY))

    private val beginningOfWeekLocalDate = endOfWeekLocalDate.map {
        val calendar = Calendar.getInstance()
        calendar.time = it.toDate()
        calendar.add(Calendar.DATE, -7)
        return@map calendar.time.toLocalDate()
    }
    //endregion Date

    val taskDueThisWeek = accountListId
        .switchCombineWith(beginningOfWeekLocalDate, endOfWeekLocalDate) { accountListId, begin, end ->
            realm.getTasks(accountListId)
                .completed(false)
                .between(TaskFields.START_AT, begin.toDate(), end.toDate()).asLiveData()
        }

    val lateCommitments = accountListId.switchMap {
        realm.getContacts(it).status(STATUS_PARTNER_FINANCIAL)
            .donationLateState(Contact.DonationLateState.ALL_LATE)
            .asLiveData()
    }

    val tasksPrayers = accountListId.switchMap {
        realm.getTasks(it).contains(TaskFields.ACTIVITY_TYPE, "Prayer").completed(false).asLiveData()
    }

    private val anniversaryPeople = accountListId
        .switchCombineWith(beginningOfWeekLocalDate, endOfWeekLocalDate) { id, begin, end ->
            val beginMonDay = MonthDay.of(begin.month, begin.dayOfMonth)
            val endMonthDay = MonthDay.of(end.month, end.dayOfMonth)
            realm.getPeople().forAccountList(id).anniversaryBetween(beginMonDay, endMonthDay).asLiveData()
        }

    private val birthdayPeople = accountListId
        .switchCombineWith(beginningOfWeekLocalDate, endOfWeekLocalDate) { id, begin, end ->
            val beginMonDay = MonthDay.of(begin.month, begin.dayOfMonth)
            val endMonthDay = MonthDay.of(end.month, end.dayOfMonth)
            realm.getPeople().forAccountList(id).birthdayBetween(beginMonDay, endMonthDay).asLiveData()
        }

    val birthdayAndAnniversaryPeople = anniversaryPeople
        .combineWith(birthdayPeople) { anniversaryPeople, birthdayPeople ->
            val people = mutableListOf<Pair<String, Person>>()
            anniversaryPeople.forEach {
                people.add(Pair(DASHBOARD_ANNIVERSARY, it))
            }

            birthdayPeople.forEach {
                people.add(Pair(DASHBOARD_BIRTHDAYS, it))
            }

            people.sortBy {
                it.second.id
            }
            return@combineWith people.toList()
        }
}
