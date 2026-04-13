package com.cylonid.nativealpha.waos.util

import android.util.Base64
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CredentialEncryption {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val SALT = "waos_credential_salt"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    private fun deriveKey(pin: String): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), SALT.toByteArray(), ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun encrypt(data: String, pin: String): String {
        val key = deriveKey(pin)
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(16)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, pin: String): String {
        val key = deriveKey(pin)
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(16)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        val decoded = Base64.decode(cipherText, Base64.NO_WRAP)
        return String(cipher.doFinal(decoded), Charsets.UTF_8)
    }
}
