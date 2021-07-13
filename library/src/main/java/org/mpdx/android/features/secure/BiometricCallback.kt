package org.mpdx.android.features.secure

import androidx.biometric.BiometricPrompt

interface BiometricCallback {
    fun onBiometricSuccess(result: BiometricPrompt.AuthenticationResult)
    fun onBiometricError(errorCode: Int, errorString: String)
    fun onBiometricFailed()
}
