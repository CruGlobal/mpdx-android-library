package org.mpdx.android.features.tasks.editor

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Parcelable
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.widget.TimePicker
import androidx.activity.addCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_NONE
import com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.kotlin.oneOf
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import org.ccci.gto.android.common.androidx.lifecycle.combineWith
import org.ccci.gto.android.common.androidx.lifecycle.observe
import org.ccci.gto.android.common.androidx.lifecycle.or
import org.ccci.gto.android.common.drawable.children
import org.ccci.gto.android.common.util.findListener
import org.mpdx.android.R
import org.mpdx.android.base.activity.BaseActivity
import org.mpdx.android.base.activity.DataBindingActivity
import org.mpdx.android.base.lifecycle.RealmModelViewModel
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.base.sync.SyncTracker
import org.mpdx.android.core.model.User
import org.mpdx.android.core.realm.forAccountList
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.core.realm.getTaskTagsFor
import org.mpdx.android.core.realm.getUser
import org.mpdx.android.core.realm.getUsers
import org.mpdx.android.core.sync.AccountListSyncService
import org.mpdx.android.core.sync.TagsSyncService
import org.mpdx.android.core.viewmodel.UserViewModel
import org.mpdx.android.databinding.TasksEditorActivityBinding
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_EDITOR_EDIT
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_EDITOR_FINISH
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_EDITOR_LOG
import org.mpdx.android.features.analytics.model.SCREEN_TASKS_EDITOR_NEW
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.contacts.model.ContactFields
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.features.contacts.realm.getContacts
import org.mpdx.android.features.contacts.realm.getPerson
import org.mpdx.android.features.contacts.realm.hasName
import org.mpdx.android.features.contacts.realm.sortByName
import org.mpdx.android.features.selector.OnItemSelectedListener
import org.mpdx.android.features.tasks.editor.widget.CutoutDrawable
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.model.TaskContactFields
import org.mpdx.android.features.tasks.newtask.TaskContactSelectorFragment
import org.mpdx.android.features.tasks.realm.createComment
import org.mpdx.android.features.tasks.realm.createTask
import org.mpdx.android.features.tasks.realm.createTaskContact
import org.mpdx.android.features.tasks.realm.forTask
import org.mpdx.android.features.tasks.realm.getTask
import org.mpdx.android.features.tasks.realm.getTaskContact
import org.mpdx.android.features.tasks.realm.getTaskContacts
import org.mpdx.android.features.tasks.sync.CommentsSyncService
import org.mpdx.android.features.tasks.sync.TasksSyncService
import org.mpdx.android.features.tasks.viewmodel.CommentViewModel
import org.mpdx.android.features.tasks.viewmodel.TaskViewModel
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.realm
import org.mpdx.android.utils.realmTransactionAsync
import org.threeten.bp.Instant
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber

private const val TAG = "TaskEditorActivity"

private const val ARG_TASK = "task"
private const val ARG_INITIAL_PROPERTIES = "initial_properties"
private const val ARG_LOG_TASK = "isLogTask"
private const val ARG_FINISH_TASK = "isFinishTask"

private const val FRAGMENT_TAG_DUE_DATE_PICKER = "dueDatePicker"
private const val FRAGMENT_TAG_COMPLETED_AT_DATE_PICKER = "completedAtPicker"

@JvmOverloads
fun Context.buildTaskEditorActivityIntent(
    taskId: String? = null,
    isLogTask: Boolean = false,
    isFinishTask: Boolean = false,
    initialProperties: TaskInitialProperties? = null
) = Intent(this, TaskEditorActivity::class.java)
    .putExtra(ARG_TASK, taskId)
    .putExtra(ARG_LOG_TASK, isLogTask)
    .putExtra(ARG_FINISH_TASK, isFinishTask)
    .putExtra(ARG_INITIAL_PROPERTIES, initialProperties)

fun Context.buildTaskEditorActivityIntentForNextAction(taskId: String) = realm {
    val task = getTask(taskId).findFirst() ?: return@realm null
    val contacts = task.getContacts(false)?.findAll()?.mapNotNull { it.id }?.toTypedArray()

    buildTaskEditorActivityIntent(
        initialProperties = TaskInitialProperties(
            subject = task.subject,
            type = task.nextAction,
            contactIds = contacts,
            dueDate = ZonedDateTime.now().plusDays(2).truncatedTo(ChronoUnit.HOURS),
            tags = task.tags?.toTypedArray()
        )
    )
}

@AndroidEntryPoint
class TaskEditorActivity :
    BaseActivity(),
    DataBindingActivity<TasksEditorActivityBinding>,
    TaskEditorCallbacks,
    OnItemSelectedListener<Contact>,
    OnUserSelectedListener {
    private inline val isFinishTask get() = intent?.getBooleanExtra(ARG_FINISH_TASK, false) ?: false
    private inline val isLogTask get() = intent?.getBooleanExtra(ARG_LOG_TASK, false) ?: false
    private inline val taskId get() = intent?.getStringExtra(ARG_TASK)
    private inline val initialProperties: TaskInitialProperties?
        get() = intent?.getParcelableExtra(ARG_INITIAL_PROPERTIES)

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUnsavedChangesDialog()
        if (savedInstanceState == null) initializeProperties()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        setupToolbar()
        mergeNotificationTimeInput()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tasks_editor_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_save -> {
            saveTask()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        attachDueDateDatePickerCallback(fragment)
        attachCompletedAtDatePickerCallback(fragment)
    }

    override fun onResume() {
        super.onResume()
        sendAnalyticsEvent()
    }

    override fun onSupportNavigateUp(): Boolean {
        // we just suppress native up navigation for a simpler finish() implementation
        if (!showUnsavedChangesDialogIfNecessary()) finish()
        return true
    }
    // endregion Lifecycle

    // region Data Binding
    override fun layoutId() = R.layout.tasks_editor_activity
    override lateinit var binding: TasksEditorActivityBinding

    override fun onCreateDataBinding(binding: TasksEditorActivityBinding) {
        super.onCreateDataBinding(binding)
        binding.isFinishTask = isFinishTask
        binding.callbacks = this
        binding.task = taskViewModel
        binding.user = UserViewModel().also { dataModel.user.observe(this, it) }
        binding.contacts = dataModel.contacts.map { contacts ->
            contacts.map { it.id to it.name }.filter { it.first != null }.toMap(LinkedHashMap())
        }
        binding.users = dataModel.users
        binding.comment = commentViewModel
        binding.tags = dataModel.tags.combineWith(taskViewModel.tagsLiveData) { tags, excluded ->
            tags.mapNotNull { it.name }.filterNot { excluded.contains(it) }
        }
    }
    // endregion Data Binding

    // region Data Model
    private val dataModel: TaskEditorActivityDataModel by lazy {
        ViewModelProvider(this).get(TaskEditorActivityDataModel::class.java)
            .also { it.taskId.value = taskId }
    }
    // endregion Data Model

    // region Task Data Model
    internal val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(this).get(TaskViewModel::class.java)
            .apply {
                allowNullModel = false
                forceUnmanaged = true
                trackingChanges = true
            }
            .also { dataModel.task.observe(this, it) }
            .also { it.contactIdsLiveData.observe(this) { ids -> dataModel.contactIds.value = ids } }
    }
    private val commentViewModel: CommentViewModel by lazy {
        ViewModelProvider(this).get(CommentViewModel::class.java).apply {
            allowNullModel = false
            forceUnmanaged = true
            trackingChanges = true
        }
    }

    private fun initializeProperties() {
        initialProperties?.applyTo(taskViewModel)
        if (isLogTask || isFinishTask) {
            taskViewModel.isCompleted = true
            taskViewModel.completedAt = taskViewModel.completedAt ?: ZonedDateTime.now()
        }
    }

    // region User Selector
    override fun showUserSelector() = TaskUserSelectorFragment().show(supportFragmentManager)

    override fun onUserSelected(user: User?) {
        dataModel.selectedUserId.value = user?.id
        dataModel.userChanged.value = true
    }
    // endregion User Selector

    // region Add Contact Selector
    override fun showAddContactSelector() {
        TaskContactSelectorFragment(*taskViewModel.contacts.ids.toTypedArray()).show(supportFragmentManager)
    }

    override fun onItemSelected(item: Contact?) {
        if (item != null) taskViewModel.contacts.addModel(if (item.isManaged) item.copyFromRealm(0) else item)
    }
    // endregion Add Contact Selector

    // region Due Date Editor
    private val dueDateSaveListener = MaterialPickerOnPositiveButtonClickListener<Long?> {
        taskViewModel.dueDateDate = it?.let { Instant.ofEpochMilli(it) }?.atZone(ZoneOffset.UTC)?.toLocalDate()
    }

    override fun showDueDateDatePicker() {
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.task_editor_hint_due_date)
            .setSelection(taskViewModel.dueDateDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
            .build().show(supportFragmentManager.beginTransaction().addToBackStack(null), FRAGMENT_TAG_DUE_DATE_PICKER)
    }

    override fun showDueDateTimePicker() {
        TaskEditorDueTimeDialogFragment().show(supportFragmentManager.beginTransaction().addToBackStack(null), null)
    }

    private fun attachDueDateDatePickerCallback(fragment: Fragment) {
        if (fragment.tag == FRAGMENT_TAG_DUE_DATE_PICKER)
            (fragment as? MaterialDatePicker<Long?>)?.addOnPositiveButtonClickListener(dueDateSaveListener)
    }

    internal class TaskEditorDueTimeDialogFragment : TaskEditorTimePickerDialogFragment() {
        override var time: LocalTime?
            get() = taskViewModel?.dueDateTime
            set(value) {
                taskViewModel?.dueDateTime = value
            }
    }
    // endregion Due Date Editor

    // region Completed At Editor
    private val completedAtSaveListener = MaterialPickerOnPositiveButtonClickListener<Long?> {
        taskViewModel.completedAtDate =
            it?.let { Instant.ofEpochMilli(it) }?.atZone(ZoneOffset.UTC)?.toLocalDate()
    }

    override fun showCompletedAtDatePicker() {
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.log_task_completed_date_string)
            .setSelection(taskViewModel.completedAtDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
            .build()
            .show(supportFragmentManager.beginTransaction().addToBackStack(null), FRAGMENT_TAG_COMPLETED_AT_DATE_PICKER)
    }

    override fun showCompletedAtTimePicker() {
        TaskEditorCompletedAtTimeDialogFragment()
            .show(supportFragmentManager.beginTransaction().addToBackStack(null), null)
    }

    private fun attachCompletedAtDatePickerCallback(fragment: Fragment) {
        if (fragment.tag == FRAGMENT_TAG_COMPLETED_AT_DATE_PICKER)
            (fragment as? MaterialDatePicker<Long?>)?.addOnPositiveButtonClickListener(completedAtSaveListener)
    }

    internal class TaskEditorCompletedAtTimeDialogFragment : TaskEditorTimePickerDialogFragment() {
        override var time: LocalTime?
            get() = taskViewModel?.completedAtTime
            set(value) {
                taskViewModel?.completedAtTime = value
            }
    }
    // endregion Completed At Editor

    // region Notification input logic
    private fun mergeNotificationTimeInput() {
        val timeDrawable = binding.notificationTimeInput.background as? MaterialShapeDrawable
        val unitDrawable = binding.notificationTimeUnitInput.background as? MaterialShapeDrawable
            ?: (binding.notificationTimeUnitInput.background as? LayerDrawable)?.children
                ?.filterIsInstance<MaterialShapeDrawable>()?.firstOrNull()
        // HACK: toggle the layout background mode to disconnect unitDrawable from the layout.
        //       This prevents flicker of the outline when toggling the unit dropdown.
        binding.notificationTimeUnit.boxBackgroundMode = BOX_BACKGROUND_NONE
        binding.notificationTimeUnit.boxBackgroundMode = BOX_BACKGROUND_OUTLINE

        // remove end edge for Time layout
        binding.notificationTimeInput.apply {
            background = null
            val timeCutout = CutoutDrawable {
                val strokeWidth = timeDrawable?.strokeWidth ?: return@CutoutDrawable
                setCutout(bounds.right - strokeWidth, strokeWidth, bounds.right.toFloat(), bounds.bottom - strokeWidth)
                unitDrawable?.setStroke(timeDrawable)
            }

            background = LayerDrawable(arrayOf(timeDrawable, timeCutout))
        }

        binding.notificationTimeUnitInput.apply {
            background = null
            val unitCutout = CutoutDrawable {
                if (unitDrawable == null) return@CutoutDrawable
                if (timeDrawable != null) unitDrawable.setStroke(timeDrawable)
                val strokeWidth = unitDrawable.strokeWidth
                setCutout(0f, strokeWidth, strokeWidth, bounds.bottom - strokeWidth)
            }

            background = LayerDrawable(arrayOf(unitDrawable, unitCutout))
        }
    }

    private fun MaterialShapeDrawable.setStroke(from: MaterialShapeDrawable) {
        if (strokeWidth != from.strokeWidth) strokeWidth = from.strokeWidth
        strokeColor = from.strokeColor
    }
    // endregion Notification input logic

    // region Save logic
    @Inject
    internal lateinit var appPrefs: AppPrefs
    @Inject
    internal lateinit var tasksSyncService: TasksSyncService
    @Inject
    internal lateinit var commentsSyncService: CommentsSyncService

    private fun saveTask() {
        if (taskViewModel.hasErrors()) {
            taskViewModel.showErrors()
            return
        }

        var savedTaskId: String? = null
        val onSuccess = {
            if (savedTaskId == null) {
                TaskSaveErrorDialogFragment().show(supportFragmentManager, TAG)
            } else {
                tasksSyncService.syncDirtyTasks().onComplete {
                    commentsSyncService.syncDirtyComments().launch()
                }.launch()
                launchNextActionIfNecessary(savedTaskId)
                setResult(RESULT_OK)
                finish()
            }
        }
        realmTransactionAsync(
            onSuccess,
            onError = {
                Timber.tag(TAG).e(it, "Error saving Task")
                TaskSaveErrorDialogFragment().show(supportFragmentManager, TAG)
            }
        ) {
            val model = taskViewModel.model ?: return@realmTransactionAsync
            val accountList = getAccountList(appPrefs.accountListId).findFirst()
            val managedTask = getTask(taskId).findFirst() ?: copyToRealm(createTask(accountList))

            managedTask.mergeChangedFields(model)
            saveUser(managedTask)
            saveContacts(managedTask, taskViewModel.contacts)
            savedTaskId = managedTask.id

            val comment = commentViewModel.model?.takeUnless { it.body.isNullOrBlank() } ?: return@realmTransactionAsync
            val managedComment = copyToRealm(createComment(managedTask, getPerson(appPrefs.userId).findFirst()))
            managedComment.mergeChangedFields(comment)
        }
    }

    private fun Realm.saveUser(task: Task) {
        if (dataModel.userChanged.value != true) return
        task.trackingChanges = true
        task.user = getUser(dataModel.selectedUserId.value).findFirst()
        task.trackingChanges = false
    }

    private fun Realm.saveContacts(task: Task, contacts: RealmModelViewModel<*>.LazyRelatedModels<Contact, *>) {
        contacts.deleted.takeUnless { it.isNullOrEmpty() }?.let { ids ->
            getTaskContacts().forTask(task.id).oneOf(TaskContactFields.CONTACT.ID, ids.toTypedArray())
                .findAll().forEach { it.isDeleted = true }
        }

        contacts.ids.forEach { contactId ->
            if (getTaskContact(task.id, contactId).findFirst() != null) return@forEach
            val contact = getContact(contactId).findFirst() ?: return@forEach
            copyToRealm(createTaskContact(task, contact))
        }
    }
    // endregion Save logic
    // endregion Task Data Model

    // region Unsaved Changes Dialog
    private fun setupUnsavedChangesDialog() {
        onBackPressedDispatcher.addCallback(this, false) { showUnsavedChangesDialog() }
            .also { c ->
                (taskViewModel.hasChangesLiveData or dataModel.userChanged).observe(this) { c.isEnabled = it == true }
            }
    }

    /**
     * @return true if the unsaved changes dialog was shown, false if it wasn't shown
     */
    private fun showUnsavedChangesDialogIfNecessary(): Boolean {
        if (taskViewModel.hasChanges) {
            showUnsavedChangesDialog()
            return true
        }
        return false
    }

    private fun showUnsavedChangesDialog() = UnsavedChangesDialogFragment()
        .show(supportFragmentManager.beginTransaction().addToBackStack("UnsavedChangesDialog"), null)

    internal class UnsavedChangesDialogFragment : DialogFragment() {
        private val parent get() = activity as? TaskEditorActivity

        override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.task_editor_unsaved_dialog)
            .setPositiveButton(R.string.task_editor_unsaved_dialog_action_save) { _, _ -> parent?.saveTask() }
            .setNegativeButton(R.string.task_editor_unsaved_dialog_action_discard) { _, _ ->
                activity?.apply {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
            .create()
    }
    // endregion Unsaved Changes Dialog

    override fun setupToolbar() {
        setTitle(
            when {
                isLogTask -> R.string.log_task_label
                isFinishTask -> R.string.task_editor_title_finish
                taskId != null -> R.string.edit_task
                else -> R.string.new_task
            }
        )
        setSupportActionBar(binding.toolbar)
        super.setupToolbar()
    }

    private fun sendAnalyticsEvent() {
        mEventBus.post(
            AnalyticsScreenEvent(
                when {
                    isLogTask -> SCREEN_TASKS_EDITOR_LOG
                    isFinishTask -> SCREEN_TASKS_EDITOR_FINISH
                    taskId != null -> SCREEN_TASKS_EDITOR_EDIT
                    else -> SCREEN_TASKS_EDITOR_NEW
                }
            )
        )
    }

    private fun launchNextActionIfNecessary(savedTaskId: String?) {
        if (savedTaskId == null) return
        if (!isLogTask && !isFinishTask) return
        if (taskViewModel.nextAction.isNullOrEmpty()) return

        startActivity(buildTaskEditorActivityIntentForNextAction(savedTaskId))
    }
}

@HiltViewModel
internal class TaskEditorActivityDataModel @Inject constructor(
    appPrefs: AppPrefs,
    private val accountListSyncService: AccountListSyncService,
    private val tasksSyncService: TasksSyncService,
    private val tagsSyncService: TagsSyncService
) : RealmViewModel() {
    private val accountListId = appPrefs.accountListIdLiveData
    val taskId = MutableLiveData<String>()
    val contactIds = MutableLiveData(emptySet<String>())

    val task = taskId.switchMap { realm.getTask(it).firstAsLiveData() }
    val contacts by lazy {
        contactIds.distinctUntilChanged().switchMap {
            realm.getContacts().oneOf(ContactFields.ID, it.toTypedArray()).hasName().sortByName().asLiveData()
        }
    }
    val tags = accountListId.switchMap { realm.getTaskTagsFor(it).asLiveData() }
    val users = accountListId.switchMap { realm.getUsers().forAccountList(it).asLiveData() }

    // region User Tracking
    val userChanged = MutableLiveData(false)
    val selectedUserId = MutableLiveData<String?>(null)
    private val selectedUser = selectedUserId.distinctUntilChanged().switchMap { realm.getUser(it).firstAsLiveData() }
    val user = userChanged.distinctUntilChanged()
        .combineWith(task, selectedUserId, selectedUser) { changed, task, selectedId, selected ->
            when {
                !changed -> task?.user
                selectedId.equals(task?.user?.id, true) -> {
                    userChanged.value = false
                    task?.user
                }
                else -> selected
            }
        }
    // endregion User Tracking

    // region Sync Logic
    val syncTracker = SyncTracker()

    init {
        accountListId.observe(this) { syncData() }
        taskId.observe(this) { syncData() }
    }

    fun syncData(force: Boolean = false) {
        taskId.value?.let { syncTracker.runSyncTasks(tasksSyncService.syncTask(it, force)) }

        val accountListId = accountListId.value ?: return
        syncTracker.runSyncTasks(
            tagsSyncService.syncTaskTags(accountListId, force),
            accountListSyncService.syncUsers(accountListId, force)
        )
    }
    // endregion Sync Logic
}

internal abstract class TaskEditorTimePickerDialogFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    protected inline val taskViewModel get() = findListener<TaskEditorActivity>()?.taskViewModel
    protected abstract var time: LocalTime?

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        TimePickerDialog(
            requireContext(), this,
            time?.hour ?: 0, time?.minute ?: 0,
            DateFormat.is24HourFormat(requireContext())
        )

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        time = LocalTime.of(hourOfDay, minute)
    }
}

@Parcelize
class TaskInitialProperties @JvmOverloads constructor(
    private val subject: String? = null,
    private val type: String? = null,
    private val contactIds: Array<String>? = null,
    private val dueDate: ZonedDateTime? = null,
    private val completedAt: Instant? = null,
    private val result: Task.Result? = null,
    private val nextAction: String? = null,
    private val tags: Array<String>? = null
) : Parcelable {
    fun applyTo(taskViewModel: TaskViewModel) {
        if (subject != null) taskViewModel.subject = subject
        if (type != null) taskViewModel.type = type
        contactIds?.forEach { taskViewModel.contacts.addModel(Contact().apply { id = it }) }
        if (dueDate != null) taskViewModel.dueDate = dueDate
        if (completedAt != null) taskViewModel.completedAt = completedAt.atZone(ZoneId.systemDefault())
        if (result != null) taskViewModel.result = result
        if (nextAction != null) taskViewModel.nextAction = nextAction
        tags?.forEach { taskViewModel.addTag(it) }
    }
}
