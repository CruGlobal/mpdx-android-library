package org.mpdx.android.core.secure

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton
import org.mpdx.android.core.secure.Crypto.decryptPbkdf2
import org.mpdx.android.core.secure.Crypto.deriveKeyPbkdf2
import org.mpdx.android.core.secure.Crypto.encrypt
import org.mpdx.android.core.secure.Crypto.generateSalt

@Singleton
class EncryptionManager @Inject constructor() : EncryptHandler() {
    @Throws(InvalidKeySpecException::class, NoSuchAlgorithmException::class)
    override fun deriveKey(password: String, salt: ByteArray): SecretKey {
        return deriveKeyPbkdf2(password, salt)
    }

    @Throws(
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        NoSuchAlgorithmException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeySpecException::class
    )
    override fun encrypt(plainText: ByteArray, password: String): String {
        val salt = generateSalt()
        val key = deriveKey(password, salt)
        return encrypt(plainText, key, salt)
    }

    @Throws(
        NoSuchPaddingException::class,
        InvalidAlgorithmParameterException::class,
        NoSuchAlgorithmException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        InvalidKeyException::class,
        InvalidKeySpecException::class
    )
    override fun decrypt(cipherText: String, password: String): ByteArray {
        return decryptPbkdf2(cipherText, password)
    }
}
