package org.mpdx.android.features.tasks.editor

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import org.mpdx.android.R

class TaskSaveErrorDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(requireContext()).apply {
        setStyle(STYLE_NO_TITLE, R.style.ThemeOverlay_Mpdx_MaterialAlertDialog)
        setMessage(R.string.task_save_error_message)
    }.create()
}
