package org.mpdx.android.features.contacts.people

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import io.realm.kotlin.oneOf
import java.util.ArrayList
import javax.inject.Inject
import org.ccci.gto.android.common.androidx.lifecycle.sortedWith
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.R
import org.mpdx.android.R2
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.core.modal.ModalFragment
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.base.fragments.BaseFragment
import org.mpdx.android.features.contacts.contactdetail.ContactDetailActivity
import org.mpdx.android.features.contacts.model.Person
import org.mpdx.android.features.contacts.model.PersonFields
import org.mpdx.android.features.contacts.realm.forAccountList
import org.mpdx.android.features.contacts.realm.getPeople
import org.threeten.bp.LocalDate
import org.threeten.bp.MonthDay
import splitties.fragmentargs.argOrNull

@AndroidEntryPoint
class PeopleByGroupingFragment() :
    BaseFragment(),
    PersonSelectedListener,
    ModalFragment {
    constructor(grouping: PeopleGrouping? = null, people: ArrayList<String>? = null) : this() {
        this.grouping = grouping
        this.people = people
    }
    private var grouping by argOrNull<PeopleGrouping>()
    private var people by argOrNull<ArrayList<String>>()

    // region Lifecycle
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
    }

    override fun onPersonSelected(person: Person?) {
        person?.contact?.id?.let { contactId ->
            activity?.run { startActivity(ContactDetailActivity.getIntent(this, contactId)) }
        }
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel by lazy {
        ViewModelProvider(this).get(PeopleByGroupingFragmentViewModel::class.java)
            .also { it.peopleIds.value = people }
    }
    // endregion Data Model

    // region Toolbar
    @JvmField
    @BindView(R2.id.people_by_grouping_toolbar)
    internal var toolbar: Toolbar? = null

    private fun setupToolbar() {
        grouping?.let { toolbar?.title = getString(it.stringResource) }
    }
    // endregion Toolbar

    // region RecyclerView
    private val adapter by lazy {
        PeopleByGroupingAdapter().also {
            it.personSelectedListener.set(this)
            dataModel.people.sortedWith(Person.birthdayComparator(MonthDay.from(LocalDate.now().minusMonths(6))))
                .observe(this, it)
        }
    }

    @JvmField
    @BindView(R2.id.people_by_grouping_recycler)
    var recyclerView: RecyclerView? = null

    private fun setupRecyclerView() {
        recyclerView?.adapter = adapter
    }
    // endregion RecyclerView

    override fun getToolbar() = toolbar
    override fun layoutRes() = R.layout.fragment_people_by_grouping
}

@HiltViewModel
class PeopleByGroupingFragmentViewModel @Inject constructor(appPrefs: AppPrefs) : RealmViewModel() {
    val accountListId = appPrefs.accountListIdLiveData
    val peopleIds = MutableLiveData<List<String>>()

    val people: LiveData<RealmResults<Person>> by lazy {
        accountListId.switchCombineWith(peopleIds) { accountListId, people ->
            realm.getPeople()
                .forAccountList(accountListId)
                .oneOf(PersonFields.ID, people?.toTypedArray().orEmpty())
                .asLiveData()
        }
    }
}
