package org.mpdx.android.features.secure

import android.app.AlertDialog.Builder
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import org.mpdx.android.R

class BiometricErrorDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Builder(context)
            .setTitle(R.string.biometric_error)
            .setNegativeButton(R.string.fingerprint_skip) { _, _ -> listener.onSkipClicked() }
            .setPositiveButton(R.string.retry) { _, _ -> listener.onRetryClicked() }
            .create()
    }
    lateinit var listener: BiometricErrorDialogListener
    interface BiometricErrorDialogListener {
        fun onRetryClicked()
        fun onSkipClicked()
    }
}
