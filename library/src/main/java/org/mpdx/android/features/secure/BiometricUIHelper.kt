package org.mpdx.android.features.secure

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricUIHelper(private var callback: BiometricCallback?) : BiometricPrompt.AuthenticationCallback() {
    fun startListening(
        activity: FragmentActivity? = null,
        fragment: Fragment? = null,
        cryptoObject: BiometricPrompt.CryptoObject,
        promptInfo: BiometricPrompt.PromptInfo
    ) {
        val context = activity ?: fragment?.context ?: return
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.USE_BIOMETRIC) != PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PERMISSION_GRANTED
        ) {
            return
        }

        fragment?.let { frag ->
            BiometricPrompt(frag, Executor { it.run() }, this).authenticate(promptInfo, cryptoObject)
            return
        }

        activity?.let { activ ->
            BiometricPrompt(activ, Executor { it.run() }, this).authenticate(promptInfo, cryptoObject)
            return
        }
    }

    fun cancel() {
        callback = null
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        callback?.onBiometricError(errorCode, errString as String)
    }

    override fun onAuthenticationFailed() {
        callback?.onBiometricFailed()
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        callback?.onBiometricSuccess(result)
    }
}
