package org.mpdx.android.core.secure

import android.util.Base64
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object Crypto {
    private const val PBKDF2_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val DELIMITER = "]"
    private const val KEY_LENGTH = 256

    // minimum values recommended by PKCS#5, increase as necessary
    private const val ITERATION_COUNT = 1000
    private const val PKCS5_SALT_LENGTH = 8
    private val random = SecureRandom()
    @JvmStatic
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun deriveKeyPbkdf2(password: String, salt: ByteArray?): SecretKey {
        val keySpec: KeySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val keyFactory = SecretKeyFactory.getInstance(PBKDF2_DERIVATION_ALGORITHM)
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun generateIv(length: Int): ByteArray {
        val b = ByteArray(length)
        random.nextBytes(b)
        return b
    }

    @JvmStatic
    fun generateSalt(): ByteArray {
        val b = ByteArray(PKCS5_SALT_LENGTH)
        random.nextBytes(b)
        return b
    }

    @JvmStatic
    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    fun encrypt(plaintext: ByteArray?, key: SecretKey?, salt: ByteArray?): String {
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val iv = generateIv(cipher.blockSize)
        val ivParams = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivParams)
        val cipherText = cipher.doFinal(plaintext)
        return if (salt != null) {
            String.format("%s%s%s%s%s", toBase64(salt), DELIMITER, toBase64(iv), DELIMITER, toBase64(cipherText))
        } else {
            String.format("%s%s%s", toBase64(iv), DELIMITER, toBase64(cipherText))
        }
    }

    @JvmStatic
    fun toHex(bytes: ByteArray): String {
        val builder = StringBuilder()
        for (b in bytes) {
            builder.append(String.format("%02X", b))
        }
        return builder.toString()
    }

    private fun toBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun fromBase64(base64: String): ByteArray {
        return Base64.decode(base64, Base64.NO_WRAP)
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    private fun decrypt(cipherBytes: ByteArray, key: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val ivParams = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, ivParams)
        return cipher.doFinal(cipherBytes)
    }

    @JvmStatic
    @Throws(
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        NoSuchAlgorithmException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeySpecException::class
    )
    fun decryptPbkdf2(cipherText: String, password: String): ByteArray {
        val fields = cipherText.split(DELIMITER).toTypedArray()
        require(fields.size == 3) { "Invalid encrypted text format" }
        val salt = fromBase64(fields[0])
        val iv = fromBase64(fields[1])
        val cipherBytes = fromBase64(fields[2])
        val key = deriveKeyPbkdf2(password, salt)
        return decrypt(cipherBytes, key, iv)
    }
}
