package org.mpdx.android.core.secure;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public abstract class EncryptHandler {
    private SecretKey key;

    public abstract SecretKey deriveKey(String password, byte[] salt)
            throws InvalidKeySpecException, NoSuchAlgorithmException;

    public abstract String encrypt(byte[] plainText, String password)
            throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidAlgorithmParameterException, InvalidKeySpecException;

    public abstract byte[] decrypt(String cipherText, String password)
            throws NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidKeySpecException;

    public String getRawKey() {
        if (key == null) {
            return null;
        }

        return Crypto.toHex(key.getEncoded());
    }
}
