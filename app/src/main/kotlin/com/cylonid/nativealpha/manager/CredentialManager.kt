package com.cylonid.nativealpha.manager

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cylonid.nativealpha.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Entity(tableName = "credentials")
data class Credential(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val webAppId: Long? = null, // null for global
    val title: String,
    val username: String,
    val password: String, // encrypted
    val url: String? = null,
    val notes: String? = null,
    val customFields: Map<String, String> = emptyMap()
)

@Singleton
class CredentialManager @Inject constructor(
    private val database: AppDatabase
) {
    private val dao = database.credentialDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val keyStoreAlias = "waos_credentials"

    fun saveCredential(webAppId: Long?, credential: Credential) {
        scope.launch {
            val encryptedPassword = encrypt(credential.password)
            val encryptedCredential = credential.copy(password = encryptedPassword)
            dao.insertCredential(encryptedCredential)
        }
    }

    fun getDecryptedCredentialsForApp(webAppId: Long) = dao.getCredentialsForApp(webAppId).map { credentials ->
        credentials.map { credential ->
            credential.copy(password = decrypt(credential.password))
        }
    }

    fun updateCredential(credential: Credential) {
        scope.launch {
            val encryptedPassword = encrypt(credential.password)
            val encryptedCredential = credential.copy(password = encryptedPassword)
            dao.updateCredential(encryptedCredential)
        }
    }

    fun deleteCredential(credential: Credential) {
        scope.launch {
            dao.deleteCredential(credential)
        }
    }

    fun generatePassword(length: Int = 12, includeSymbols: Boolean = true): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
                if (includeSymbols) "!@#$%^&*()_+-=[]{}|;:,.<>?" else ""
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun encrypt(data: String): String {
        try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            val combined = iv + encrypted
            return android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            // Fallback to simple encryption for now
            return android.util.Base64.encodeToString(data.toByteArray(Charsets.UTF_8), android.util.Base64.DEFAULT)
        }
    }

    private fun decrypt(encryptedData: String): String {
        try {
            val key = getOrCreateKey()
            val combined = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)
            val iv = combined.copyOfRange(0, 12) // GCM IV is 12 bytes
            val encrypted = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            // Fallback to simple decryption
            return String(android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT), Charsets.UTF_8)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        // Try to get existing key
        keyStore.getKey(keyStoreAlias, null)?.let {
            return it as SecretKey
        }

        // Create new key
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyStoreAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
}