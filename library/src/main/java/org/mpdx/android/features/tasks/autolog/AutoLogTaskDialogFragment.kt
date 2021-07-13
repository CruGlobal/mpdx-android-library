package org.mpdx.android.features.tasks.autolog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.mpdx.android.R
import org.mpdx.android.base.fragment.BaseDialogFragment
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.core.realm.getAccountList
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.analytics.model.TaskAutoLogAddInfoAction
import org.mpdx.android.features.analytics.model.TaskAutoLogCancelAction
import org.mpdx.android.features.analytics.model.TaskAutoLogOkAction
import org.mpdx.android.features.analytics.model.TaskLogAction
import org.mpdx.android.features.contacts.realm.getContact
import org.mpdx.android.features.tasks.editor.TaskInitialProperties
import org.mpdx.android.features.tasks.editor.buildTaskEditorActivityIntent
import org.mpdx.android.features.tasks.editor.buildTaskEditorActivityIntentForNextAction
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.realm.createTask
import org.mpdx.android.features.tasks.realm.createTaskContact
import org.mpdx.android.features.tasks.realm.getTask
import org.mpdx.android.features.tasks.sync.TasksSyncService
import org.mpdx.android.features.tasks.taskdetail.AllowedActivityTypes
import org.mpdx.android.utils.realmTransactionAsync
import org.mpdx.android.utils.toBpInstant
import org.threeten.bp.ZonedDateTime
import splitties.fragmentargs.arg
import splitties.fragmentargs.argOrNull

@AndroidEntryPoint
class AutoLogTaskDialogFragment() : BaseDialogFragment() {
    constructor(type: AllowedActivityTypes, contactId: String? = null, taskId: String? = null) : this() {
        this.type = type
        this.contactId = contactId
        this.taskId = taskId
    }

    private var type by arg<AllowedActivityTypes>()
    private var contactId by argOrNull<String>()
    private var taskId by argOrNull<String>()

    @Inject
    internal lateinit var appPrefs: AppPrefs

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataModel.contact.observe(this) { updateMessage() }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
            .setMessage(buildMessage())
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                eventBus.post(TaskAutoLogOkAction)
                recordTask(dialog)
            }
            .setNeutralButton(R.string.return_to_app_add_info) { dialog, _ ->
                eventBus.post(TaskAutoLogAddInfoAction)
                recordTask(dialog, true)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                eventBus.post(
                    TaskAutoLogCancelAction
                )
            }
        if (type == AllowedActivityTypes.CALL) {
            builder.setSingleChoiceItems(
                arrayOf(
                    getString(R.string.result_attempted_left_message),
                    getString(R.string.result_attempted),
                    getString(R.string.result_completed),
                    getString(R.string.result_appointment_scheduled)
                ),
                0,
                null
            )
        }
        return builder.create()
    }
    // endregion Lifecycle

    // region Data Model
    private val dataModel by lazy {
        ViewModelProvider(this).get(AutoLogTaskDialogFragmentDataModel::class.java)
            .also { it.contactId.value = contactId }
    }
    // endregion Data Model

    // region Title
    private fun buildMessage() =
        getString(R.string.return_to_app_title, getString(type.stringRes), dataModel.contact.value?.name ?: "")

    private fun updateMessage() = dialog?.setTitle(buildMessage())
    // endregion Title

    // region Record Task
    @Inject
    internal lateinit var tasksSyncService: TasksSyncService

    private fun recordTask(dialog: DialogInterface, addInfo: Boolean = false) {
        var result: Task.Result? = Task.Result.COMPLETED
        var nextAction: String? = null

        if (type == AllowedActivityTypes.CALL) {
            when (val position = (dialog as? AlertDialog)?.listView?.checkedItemPosition) {
                null -> Unit
                3 -> nextAction = Task.ACTIVITY_TYPE_APPOINTMENT
                else -> result = Task.Result.fromValue(Task.RESULTS_CALL[position])
            }
        }

        if (addInfo) {
            showAddInfoTaskModal(result, nextAction)
        } else {
            saveTask(result, nextAction)
        }
    }

    private fun saveTask(result: Task.Result?, nextAction: String?) {
        val activity = activity
        var savedId: String? = null

        realmTransactionAsync(
            onSuccess = {
                tasksSyncService.syncDirtyTasks().launch()
                savedId?.let { activity?.finishSaveTask(it, nextAction) }
            }
        ) {
            val task = when {
                // existing task we are updating
                taskId != null -> getTask(this@AutoLogTaskDialogFragment.taskId).findFirst()?.apply {
                    trackingChanges = true
                }
                // new auto-log task
                else -> copyToRealm(createTask(getAccountList(appPrefs.accountListId).findFirst())).apply {
                    activityType = type.apiValue
                    getContact(contactId).findFirst()?.let { copyToRealm(createTaskContact(this, it)) }
                    startAt = appPrefs.appStartedTime
                }
            } ?: return@realmTransactionAsync

            task.isCompleted = true
            task.completedAt = ZonedDateTime.now()
            task.result = result
            task.nextAction = nextAction

            eventBus.post(TaskLogAction)
            savedId = task.id
        }
    }

    private fun Activity.finishSaveTask(savedId: String, nextAction: String?) {
        // trigger next action if defined
        if (nextAction != null) startActivity(buildTaskEditorActivityIntentForNextAction(savedId))

        // show success message
        Toast.makeText(this, R.string.task_logged, Toast.LENGTH_SHORT).show()
        finishTaskDetailsActivity()
    }
    // endregion Record Task

    private fun showAddInfoTaskModal(result: Task.Result?, nextAction: String?) {
        val startedTime = appPrefs.appStartedTime?.toBpInstant()
        val properties = when {
            taskId != null -> {
                TaskInitialProperties(
                    completedAt = startedTime,
                    result = result,
                    nextAction = nextAction
                )
            }
            else -> {
                TaskInitialProperties(
                    type = type.apiValue,
                    contactIds = contactId?.let { arrayOf(it) },
                    completedAt = startedTime,
                    result = result,
                    nextAction = nextAction
                )
            }
        }

        activity?.run {
            finishTaskDetailsActivity()
            startActivity(
                buildTaskEditorActivityIntent(
                    this@AutoLogTaskDialogFragment.taskId,
                    isLogTask = true,
                    initialProperties = properties
                )
            )
        }
    }

    @Deprecated("This should be handled within the TaskDetailsActivity itself")
    private fun Activity.finishTaskDetailsActivity() {
        // TODO: this only happens for the TaskDetailsActivity and should be handled within that activity via a
        //       generic callback
        if (this@AutoLogTaskDialogFragment.taskId != null) finish()
    }
}

@HiltViewModel
class AutoLogTaskDialogFragmentDataModel @Inject constructor() : RealmViewModel() {
    val contactId = MutableLiveData<String?>()

    val contact = contactId.distinctUntilChanged().switchMap { id -> realm.getContact(id).firstAsLiveData() }
}
