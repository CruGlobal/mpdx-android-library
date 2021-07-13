package org.mpdx.android.features.secure

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ccci.gto.android.common.api.okhttp3.SessionApiException
import org.mpdx.android.R
import org.mpdx.android.base.AuthenticationListener
import org.mpdx.android.core.UserApi
import org.mpdx.android.core.modal.ModalFragment
import org.mpdx.android.core.model.DbUser
import org.mpdx.android.core.model.User
import org.mpdx.android.core.realm.RealmManager
import org.mpdx.android.databinding.FragmentUnlockBinding
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.MainActivity
import org.mpdx.android.features.base.fragments.BindingFragment
import org.mpdx.android.features.onboarding.EnrollPinView.MIN_PIN_LENGTH
import org.mpdx.android.features.secure.BiometricErrorDialogFragment.BiometricErrorDialogListener
import org.mpdx.android.utils.copyFromRealm
import org.mpdx.android.utils.hideKeyboard
import org.mpdx.android.utils.realm
import timber.log.Timber

private const val TAG = "UnlockFragment"

@AndroidEntryPoint
class UnlockFragment :
    BindingFragment<FragmentUnlockBinding>(),
    ModalFragment,
    BiometricCallback {
    @Inject
    internal lateinit var appPrefs: AppPrefs
    @Inject
    internal lateinit var lazyRealmManager: Lazy<RealmManager>
    private inline val realmManager get() = lazyRealmManager.get()
    @Inject
    internal lateinit var securityManager: SecurityManager
    @Inject
    internal lateinit var authenticationListener: AuthenticationListener
    @Inject
    internal lateinit var lazyUserApi: Lazy<UserApi>
    private inline val userApi get() = lazyUserApi.get()

    // region Lifecycle
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFingerprint()
        binding.pinEditText.setOnEditorActionListener { _, i, _ -> onEditTextAction(i) }
        binding.unlockPinButton.setOnClickListener { unlockWithPin() }
    }

    override fun onResume() {
        super.onResume()
        startListeningForFingerprint()
    }

    override fun onPause() {
        super.onPause()
        stopListeningForFingerprint()
    }
    // endregion Lifecycle

    // region Fingerprint
    private inline val hasFingerprint get() = !appPrefs.realmKeySecuredWithFingerprint.isNullOrEmpty()

    private fun setupFingerprint() {
        binding.hasBiometric = hasFingerprint
        if (hasFingerprint) {
            binding.fingerprintEnabled.setOnClickListener { startListeningForFingerprint() }
        }
    }

    private fun startListeningForFingerprint() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (!hasFingerprint) return
        binding.listeningForBiometric = securityManager.startListeningForBiometrics(this, this, Cipher.DECRYPT_MODE)
    }

    private fun stopListeningForFingerprint() {
        securityManager.cancelListeningForBiometric()
        binding.listeningForBiometric = false
    }
    // endregion Fingerprint

    override fun layoutRes() = R.layout.fragment_unlock
    override fun getToolbar() = null
    private inline fun startSplashActivityAndFinish() {
        activity?.run {
            authenticationListener.startSplashActivity(this)
            finish()
        }
    }

    private fun onEditTextAction(actionId: Int): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            unlockWithPin()
            return true
        }
        return false
    }

    private fun unlockWithPin() {
        if (appPrefs.invalidKeyCount >= INVALID_KEY_MAX) {
            authenticationListener.logOutOfSession(requireActivity())
            realmManager.deleteRealm(true)
            appPrefs.accountListId = null
            appPrefs.resetInvalidKeyCount()
            appPrefs.resetRealmKey()
        } else if (binding.pinEditText.text.length >= MIN_PIN_LENGTH) {
            val isPinValid = securityManager.unlockWithPin(binding.pinEditText.text.toString())
            if (isPinValid) {
                stopListeningForFingerprint()
                binding.pinEditText.hideKeyboard()
                binding.unlockingProgressBar.visibility = View.VISIBLE
                appPrefs.resetInvalidKeyCount()
                updateUserFromApi()
            } else {
                binding.pinEditText.setText("")
                appPrefs.incrementInvalidKeyCount()
                Toast.makeText(context, R.string.incorrect_pin, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, R.string.invalid_pin, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserFromApi() {
        // there isn't a logged in user
        if (authenticationListener.getSessionGuid() == null) return startSplashActivityAndFinish()

        lifecycleScope.launch(Dispatchers.Main) {
            val user = try {
                userApi.getUser().takeIf { it.isSuccessful }?.body()?.dataSingle
            } catch (e: SessionApiException) {
                // The user's session has completely expired.
                authenticationListener.logOutOfSession(requireActivity())
                context?.run { Toast.makeText(this, R.string.message_auth_expired, Toast.LENGTH_SHORT).show() }
                return@launch startSplashActivityAndFinish()
            } catch (suppressed: IOException) {
                null
            }

            if (user != null) updateUser(user)
            when {
                !user?.setupStatus.isNullOrEmpty() -> showUserAccountStatusError(user?.setupStatus)
                else -> continueToApplication()
            }
        }
    }

    private fun updateUser(user: User) {
        appPrefs.userId = user.id
        appPrefs.accountListId = appPrefs.accountListId?.takeIf { it.isNotEmpty() } ?: user.defaultAccountList

        // reset cache if the user changed for some reason
        val users = realm { where(DbUser::class.java).findAll().copyFromRealm(0) }
        val dbUser = users.firstOrNull()
        if (users.size > 1 || (dbUser != null && dbUser.id != user.id)) realmManager.deleteRealm(false)
    }

    private fun showUserAccountStatusError(userAccountStatus: String?) {
        when (userAccountStatus) {
            NO_ACCOUNT_LISTS -> showAccountStatusErrorDialog(getString(R.string.no_account_list_error_message))
            NO_DEFAULT_ACCOUNT_LIST ->
                showAccountStatusErrorDialog(getString(R.string.no_default_account_list_error_message))
            NO_ORGANIZATION_OR_DEFAULT_ACCOUNT_LIST ->
                showAccountStatusErrorDialog(getString(R.string.no_organization_or_default_account_list_error_message))
            else -> Timber.e("User account status is not recognized: %s", userAccountStatus)
        }
    }

    private fun showAccountStatusErrorDialog(message: String) {
        val context = context ?: return
        AlertDialog.Builder(context)
            .setTitle(ACCOUNT_STATUS_ERROR)
            .setMessage(message)
            .setPositiveButton(R.string.okay) { _, _ ->
                authenticationListener.logOutOfSession(requireActivity())
                realmManager.deleteRealm(false)
                appPrefs.accountListId = null
                startSplashActivityAndFinish()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    private fun continueToApplication() = activity?.run {
        startActivity(MainActivity.getIntent(activity, arguments?.getString(ARG_DEEP_LINK_TYPE), deepLinkId))
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBiometricSuccess(result: BiometricPrompt.AuthenticationResult) {
        if (securityManager.unlockWithBiometric()) {
            updateUserFromApi()
        } else {
            Timber.e("Biometric Authorization error")
            val dialogFragment = BiometricErrorDialogFragment()
            dialogFragment.listener = object : BiometricErrorDialogListener {
                override fun onRetryClicked() {
                    securityManager
                        .startListeningForBiometrics(this@UnlockFragment, this@UnlockFragment, Cipher.DECRYPT_MODE)
                }

                override fun onSkipClicked() {}
            }
            fragmentManager?.beginTransaction()?.let { dialogFragment.show(it, "BiometricErrorDialogFragment") }
        }
    }

    override fun onBiometricError(errorCode: Int, errorString: String) {
        Timber.tag(TAG).e("Fingerprint enrollment error: %d %s", errorCode, errorString)
    }

    override fun onBiometricFailed() {
        Timber.tag(TAG).e("Fingerprint enrollment failed")
        Toast.makeText(context, R.string.fingerprint_failed, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val NO_ACCOUNT_LISTS = "no account_lists"
        private const val NO_DEFAULT_ACCOUNT_LIST = "no default_account_list"
        private const val NO_ORGANIZATION_OR_DEFAULT_ACCOUNT_LIST = "no organization_account on default_account_list"
        private const val ACCOUNT_STATUS_ERROR = "Error"
        private const val INVALID_KEY_MAX = 3

        @JvmStatic
        fun create(deepLinkType: String?, deepLinkId: String?, deepLinkTime: Long): Fragment {
            val fragment = UnlockFragment()
            fragment.setDeepLinkArguments(deepLinkType, deepLinkId, deepLinkTime)
            return fragment
        }
    }
}
