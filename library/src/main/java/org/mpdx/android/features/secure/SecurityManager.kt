package org.mpdx.android.features.secure

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProperties.BLOCK_MODE_CBC
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_PKCS7
import android.security.keystore.KeyProperties.KEY_ALGORITHM_AES
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.common.base.Throwables
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.ProviderException
import java.security.SecureRandom
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.security.spec.InvalidKeySpecException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import org.greenrobot.eventbus.EventBus
import org.mpdx.android.R
import org.mpdx.android.core.realm.RealmManager
import org.mpdx.android.core.secure.EncryptionManager
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.analytics.model.ACTION_CRYPTO_FAILURE
import org.mpdx.android.features.analytics.model.ACTION_FINGERPRINT_ENABLED
import org.mpdx.android.features.analytics.model.ACTION_FINGERPRINT_SCANNER
import org.mpdx.android.features.analytics.model.AnalyticsActionEvent
import org.mpdx.android.features.analytics.model.CATEGORY_SETUP
import timber.log.Timber

@Singleton
class SecurityManager @Inject constructor(
    private val context: Context,
    private val appPrefs: AppPrefs,
    private val eventBus: EventBus,
    private val mRealmManager: RealmManager,
    private val encryptionManager: EncryptionManager
) {
    private var biometricUIHelper: BiometricUIHelper? = null
    private var biometricCryptoObject: BiometricPrompt.CryptoObject? = null
    private var biometricPromptInfo: PromptInfo? = null
    fun enrollWithPin(pin: String?): Boolean {
        return try {
            val key = ByteArray(64)
            SecureRandom().nextBytes(key)
            mRealmManager.deleteRealm(true)
            if (!mRealmManager.unlockRealm(key)) {
                return false
            }
            pin ?: return false
            val encryptedKey = encryptionManager.encrypt(key, pin)
            appPrefs.realmKeySecuredWithPin = encryptedKey
            true
        } catch (e: java.lang.Exception) {
            when (e) {
                is NoSuchPaddingException,
                is InvalidKeyException,
                is IllegalBlockSizeException,
                is BadPaddingException,
                is InvalidAlgorithmParameterException,
                is InvalidKeySpecException, is NoSuchAlgorithmException -> {
                    Timber.e(e)
                    eventBus.post(AnalyticsActionEvent(ACTION_CRYPTO_FAILURE, CATEGORY_SETUP, e.message))
                    return false
                }
                else -> throw e
            }
        }
    }

    fun unlockWithPin(pin: String?): Boolean {
        val encryptedKey = appPrefs.realmKeySecuredWithPin
        try {
            encryptedKey ?: return false
            pin ?: return false
            val key = encryptionManager.decrypt(encryptedKey, pin)
            return mRealmManager.unlockRealm(key)
        } catch (e: Exception) {
            when (e) {
                is NoSuchPaddingException,
                is InvalidAlgorithmParameterException,
                is IllegalBlockSizeException,
                is NoSuchAlgorithmException,
                is InvalidKeyException,
                is InvalidKeySpecException -> {
                    eventBus.post(AnalyticsActionEvent(ACTION_CRYPTO_FAILURE, CATEGORY_SETUP, e.message))
                    Timber.e(e)
                }
                is BadPaddingException -> Timber.i(e)
                else -> throw e
            }
        }
        return false
    }

    fun startListeningForBiometrics(activity: FragmentActivity?, callback: BiometricCallback, mode: Int): Boolean {
        Timber.i("startListeningForBiometrics() called with: mode = [%d]", mode)
        try {
            if (createBiometricHelper(callback, mode)) {
                val biometricCryptoObject = biometricCryptoObject ?: return false
                val biometricPromptInfo = biometricPromptInfo ?: return false
                biometricUIHelper?.startListening(activity, null, biometricCryptoObject, biometricPromptInfo)
                    ?: return false
            } else {
                return false
            }
        } catch (e: Exception) {
            when (e) {
                is KeyStoreException -> sendErrorMessage(e, "Failed to get an instance of Keystore")
                is NoSuchAlgorithmException,
                is NoSuchProviderException -> sendErrorMessage(e, "Failed to get an instance of KeyGenerator")
                is NoSuchPaddingException -> sendErrorMessage(e, "Failed to get instance of Cipher")
                is CertificateException,
                is UnrecoverableKeyException,
                is InvalidKeyException,
                is IOException,
                is InvalidAlgorithmParameterException -> sendErrorMessage(e, "Failed to init Cipher")
                else -> throw e
            }
        }
        return true
    }

    fun startListeningForBiometrics(fragment: Fragment?, callback: BiometricCallback, mode: Int): Boolean {
        Timber.i("startListeningForBiometrics() called with: mode = [%d]", mode)
        try {
            if (createBiometricHelper(callback, mode)) {
                val bioCryptoObject = biometricCryptoObject ?: return false
                val bioPromptInfo = biometricPromptInfo ?: return false
                biometricUIHelper?.startListening(null, fragment, bioCryptoObject, bioPromptInfo)
                    ?: return false
            } else {
                return false
            }
        } catch (e: Exception) {
            when (e) {
                is KeyStoreException -> sendErrorMessage(e, "Failed to get an instance of Keystore")
                is NoSuchAlgorithmException,
                is NoSuchProviderException -> sendErrorMessage(e, "Failed to get an instance of KeyGenerator")
                is NoSuchPaddingException -> sendErrorMessage(e, "Failed to get instance of Cipher")
                is CertificateException,
                is UnrecoverableKeyException,
                is IOException,
                is InvalidAlgorithmParameterException -> sendErrorMessage(e, "Failed to init Cipher")
                // This is not an error it is caused if user adds another fingerPrint
                is InvalidKeyException -> sendErrorMessage(e, "Failed to init Cipher", Log.INFO)
                else -> throw e
            }
            return false
        }
        return true
    }

    private fun sendErrorMessage(e: Exception, errorMessage: String, logLevel: Int = Log.ERROR) {
        eventBus.post(AnalyticsActionEvent(ACTION_CRYPTO_FAILURE, CATEGORY_SETUP, e.message))
        Timber.log(logLevel, e, errorMessage)
    }

    @Throws(
        CertificateException::class,
        NoSuchPaddingException::class,
        UnrecoverableKeyException::class,
        NoSuchAlgorithmException::class,
        IOException::class,
        KeyStoreException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        NoSuchProviderException::class
    )
    private fun createBiometricHelper(callback: BiometricCallback, mode: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        Timber.i("createBiometricHelper() called with: mode = [%d]", mode)
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        // If we don't have a stored key, generate a new one
        var storedKey: Key? = null
        try {
            storedKey = keyStore.getKey(KEY_NAME, null)
        } catch (e: UnrecoverableKeyException) {
            Timber.e(e)
        }
        if (storedKey == null) {
            val builder =
                KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(ENCRYPTION_PADDING_PKCS7)
            try {
                val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, "AndroidKeyStore")
                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            } catch (e: ProviderException) {
                Throwables.propagateIfPossible(e.cause, KeyStoreException::class.java)
                throw e
            }
            storedKey = keyStore.getKey(KEY_NAME, null)
        }
        val cipher: Cipher?
        if (storedKey !is SecretKey) {
            throw UnrecoverableKeyException("Unable to recover secret key, was null from keyStore")
        }
        cipher = createCipherForBiometric(mode, storedKey)
        if (cipher == null) {
            return false
        }
        biometricCryptoObject = BiometricPrompt.CryptoObject(cipher)
        biometricPromptInfo = PromptInfo.Builder()
            .setTitle(context.getString(R.string.app_name))
            .setDescription(context.getString(R.string.biometric_dialog_touch_sensor))
            .setConfirmationRequired(true)
            .setNegativeButtonText(context.getString(R.string.cancel))
            .build()
        biometricUIHelper = BiometricUIHelper(callback)
        return true
    }

    fun cancelListeningForBiometric() {
        if (biometricUIHelper != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            biometricUIHelper!!.cancel()
            biometricUIHelper = null
        }
        biometricCryptoObject = null
        biometricPromptInfo = null
    }

    @SuppressLint("BinaryOperationInTimber")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Throws(
        CertificateException::class,
        NoSuchAlgorithmException::class,
        IOException::class,
        UnrecoverableKeyException::class,
        KeyStoreException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        NoSuchPaddingException::class
    )
    private fun createCipherForBiometric(mode: Int, secretKey: SecretKey): Cipher? {
        val cipher = Cipher.getInstance("$KEY_ALGORITHM_AES/$BLOCK_MODE_CBC/$ENCRYPTION_PADDING_PKCS7")
        if (mode == Cipher.ENCRYPT_MODE) {
            cipher.init(mode, secretKey)
            appPrefs.secureIv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        } else {
            val iv = Base64.decode(appPrefs.secureIv, Base64.NO_WRAP)
            val ivSpec = IvParameterSpec(iv)
            try {
                cipher.init(mode, secretKey, ivSpec)
            } catch (e: NullPointerException) {
                Timber
                    .tag("SecurityManager")
                    .i(e, "Cipher.init($mode, %s, %s)", "${secretKey.encoded?.size} bytes", "${ivSpec.iv.size} bytes")
                return null
            }
        }
        return cipher
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Throws(BadPaddingException::class)
    fun enrollWithBiometric(pin: String?): Boolean {
        try {
            val encryptedKey = appPrefs.realmKeySecuredWithPin ?: return false
            pin ?: return false
            val key = encryptionManager.decrypt(encryptedKey, pin)
            if (!mRealmManager.unlockRealm(key)) {
                return false
            }
            val encryptedKeyFingerprint = encryptWithBiometric(key)
            appPrefs.realmKeySecuredWithFingerprint = encryptedKeyFingerprint
            return true
        } catch (e: Exception) {
            when (e) {
                is IllegalBlockSizeException,
                is NoSuchAlgorithmException,
                is InvalidKeyException,
                is InvalidAlgorithmParameterException,
                is InvalidKeySpecException,
                is NoSuchPaddingException -> {
                    eventBus.post(AnalyticsActionEvent(ACTION_CRYPTO_FAILURE, CATEGORY_SETUP, e.message))
                    Timber.e(e)
                }
                else -> throw e
            }
            return false
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun unlockWithBiometric(): Boolean {
        return try {
            val encryptedKey = appPrefs.realmKeySecuredWithFingerprint
            val key = decryptWithBiometric(encryptedKey) ?: return false
            mRealmManager.unlockRealm(key)
        } catch (e: Exception) {
            when (e) {
                is BadPaddingException,
                is IllegalBlockSizeException -> {
                    eventBus.post(AnalyticsActionEvent(ACTION_CRYPTO_FAILURE, CATEGORY_SETUP, e.message))
                    Timber.e(e, "Failed to decrypt")
                    false
                }
                else -> throw e
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Throws(BadPaddingException::class, IllegalBlockSizeException::class)
    private fun encryptWithBiometric(byteArrayToEncrypt: ByteArray): String {
        Timber.i("encryptWithBiometric() called with: byteArrayToEncrypt size = [%d]", byteArrayToEncrypt.size)
        val encryptedBytes = biometricCryptoObject!!.cipher!!.doFinal(byteArrayToEncrypt)
        Timber.i("encryptWithBiometric() called with: encryptedBytes size = [%d]", encryptedBytes.size)
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Throws(BadPaddingException::class, IllegalBlockSizeException::class)
    fun decryptWithBiometric(encryptedString: String?): ByteArray? {
        if (biometricCryptoObject == null) {
            return null
        }
        val encryptedBytes = Base64.decode(encryptedString, Base64.NO_WRAP)
        return biometricCryptoObject!!.cipher!!.doFinal(encryptedBytes)
    }

    fun deviceHasBiometricSupport(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        if (
            ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_BIOMETRIC) != PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PERMISSION_GRANTED
        ) {
            return false
        }
        val manager = BiometricManager.from(context)
        if (manager == null) {
            // Device doesn't support fingerprint authentication
            eventBus.post(AnalyticsActionEvent(ACTION_FINGERPRINT_SCANNER, CATEGORY_SETUP, "false"))
            return false
        } else if (manager.canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
            // User hasn't enrolled any fingerprints to authenticate with
            eventBus.post(AnalyticsActionEvent(ACTION_FINGERPRINT_SCANNER, CATEGORY_SETUP, "true"))
            eventBus.post(AnalyticsActionEvent(ACTION_FINGERPRINT_ENABLED, CATEGORY_SETUP, "false"))
            return false
        }
        eventBus.post(AnalyticsActionEvent(ACTION_FINGERPRINT_SCANNER, CATEGORY_SETUP, "true"))
        eventBus.post(AnalyticsActionEvent(ACTION_FINGERPRINT_ENABLED, CATEGORY_SETUP, "true"))
        return true
    }

    companion object {
        private const val KEY_NAME = "MPDX_KEY"
    }
}
