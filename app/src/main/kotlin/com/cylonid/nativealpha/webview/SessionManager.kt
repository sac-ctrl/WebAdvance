package com.cylonid.nativealpha.webview

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.webkit.CookieManager
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * WAOS Multi-Account Session Isolation System
 *
 * Each app instance has completely separate:
 * - Cookies (via setDataDirectorySuffix per-process)
 * - LocalStorage / SessionStorage (via JS injection)
 * - IndexedDB / Cache (via separate WebView data directory)
 *
 * Sessions can be exported to AES-256-GCM encrypted JSON and imported back.
 */
class SessionManager(
    private val context: Context,
    private val appId: Long,
    private val appName: String
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val sessionDir = File(context.filesDir, "waos_sessions/$appId")
    private val keyAlias = "waos_session_key_$appId"
    private val androidKeyStore = "AndroidKeyStore"

    companion object {
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        /**
         * CRITICAL: Call this ONCE per process before any WebView is created.
         * Sets separate data directory so cookies, localStorage, IndexedDB, cache
         * are completely isolated per app — no sharing between apps.
         */
        fun applyIsolation(appId: Long) {
            try {
                WebView.setDataDirectorySuffix("app_profile_$appId")
            } catch (e: Exception) {
                // Already set in this process — safe to ignore
            }
        }

        /**
         * Build the JS snippet that extracts localStorage as a JSON string.
         */
        fun buildLocalStorageExtractJs(): String = """
            (function() {
                try {
                    var obj = {};
                    for (var i = 0; i < localStorage.length; i++) {
                        var k = localStorage.key(i);
                        obj[k] = localStorage.getItem(k);
                    }
                    return JSON.stringify(obj);
                } catch(e) { return '{}'; }
            })()
        """.trimIndent()

        /**
         * Build the JS snippet that extracts sessionStorage as a JSON string.
         */
        fun buildSessionStorageExtractJs(): String = """
            (function() {
                try {
                    var obj = {};
                    for (var i = 0; i < sessionStorage.length; i++) {
                        var k = sessionStorage.key(i);
                        obj[k] = sessionStorage.getItem(k);
                    }
                    return JSON.stringify(obj);
                } catch(e) { return '{}'; }
            })()
        """.trimIndent()

        /**
         * Build JS that restores localStorage from a JSON map.
         */
        fun buildLocalStorageRestoreJs(jsonMap: String): String = """
            (function() {
                try {
                    var obj = JSON.parse('${jsonMap.replace("'", "\\'")}');
                    Object.entries(obj).forEach(function(entry) {
                        localStorage.setItem(entry[0], entry[1]);
                    });
                } catch(e) {}
            })()
        """.trimIndent()

        /**
         * Build JS that restores sessionStorage from a JSON map.
         */
        fun buildSessionStorageRestoreJs(jsonMap: String): String = """
            (function() {
                try {
                    var obj = JSON.parse('${jsonMap.replace("'", "\\'")}');
                    Object.entries(obj).forEach(function(entry) {
                        sessionStorage.setItem(entry[0], entry[1]);
                    });
                } catch(e) {}
            })()
        """.trimIndent()
    }

    init {
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
    }

    // -------------------------------------------------------------------------
    // COOKIE MANAGEMENT
    // -------------------------------------------------------------------------

    /**
     * Extract all cookies for the given URL from CookieManager.
     * Returns a list of cookie attribute maps.
     */
    fun extractCookies(url: String): List<Map<String, String>> {
        val cookieString = CookieManager.getInstance().getCookie(url) ?: return emptyList()
        return cookieString.split(";").mapNotNull { part ->
            val trimmed = part.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx < 0) {
                mapOf("name" to trimmed, "value" to "", "domain" to extractDomain(url))
            } else {
                mapOf(
                    "name" to trimmed.substring(0, eqIdx).trim(),
                    "value" to trimmed.substring(eqIdx + 1).trim(),
                    "domain" to extractDomain(url),
                    "path" to "/"
                )
            }
        }
    }

    /**
     * Inject cookies into CookieManager BEFORE loading any URL.
     * Call CookieManager.flush() after to ensure they are persisted.
     */
    fun injectCookies(cookies: List<Map<String, String>>) {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cookies.forEach { cookie ->
            val domain = cookie["domain"] ?: return@forEach
            val name = cookie["name"] ?: return@forEach
            val value = cookie["value"] ?: ""
            val path = cookie["path"] ?: "/"
            val secure = cookie["secure"]?.lowercase() == "true"
            val cookieStr = buildString {
                append("$name=$value")
                append("; path=$path")
                if (cookie["expiry"]?.isNotEmpty() == true) {
                    append("; expires=${cookie["expiry"]}")
                }
                if (secure) append("; Secure")
                if (cookie["httpOnly"]?.lowercase() == "true") append("; HttpOnly")
                if (domain.startsWith(".")) append("; Domain=$domain")
            }
            cm.setCookie("https://$domain", cookieStr)
            cm.setCookie("http://$domain", cookieStr)
        }
        cm.flush()
    }

    // -------------------------------------------------------------------------
    // SESSION EXPORT
    // -------------------------------------------------------------------------

    /**
     * Build a complete session snapshot object.
     * The WebView must inject JS results for localStorage/sessionStorage.
     */
    fun buildSessionSnapshot(
        url: String,
        userAgent: String,
        localStorageJson: String,
        sessionStorageJson: String
    ): JsonObject {
        val cookies = extractCookies(url)
        val snapshot = JsonObject().apply {
            addProperty("appId", appId)
            addProperty("appName", appName)
            addProperty("url", url)
            addProperty("userAgent", userAgent)
            addProperty("timestamp", System.currentTimeMillis())
            add("cookies", gson.toJsonTree(cookies))
            add("localStorage", parseWebViewJsonResult(localStorageJson))
            add("sessionStorage", parseWebViewJsonResult(sessionStorageJson))
        }
        return snapshot
    }

    /**
     * Decode a value returned by `WebView.evaluateJavascript`. The WebView
     * JSON-encodes whatever the JS expression returned, so a JS string of
     * `{"k":"v"}` arrives here as the literal text `"{\"k\":\"v\"}"`.
     * Naive unescaping breaks when values themselves contain quoted JSON
     * (e.g. `marketing_attribution`), so we let Gson do the decoding twice:
     * once to strip the WebView wrapper, then once on the inner JSON.
     */
    private fun parseWebViewJsonResult(raw: String?): JsonElement {
        val empty = JsonObject()
        if (raw.isNullOrBlank() || raw == "null" || raw == "undefined") return empty
        return try {
            val element = JsonParser.parseString(raw)
            when {
                element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                    val inner = element.asString
                    if (inner.isBlank()) empty
                    else try {
                        JsonParser.parseString(inner)
                    } catch (_: Exception) { empty }
                }
                element.isJsonObject || element.isJsonArray -> element
                else -> empty
            }
        } catch (_: Exception) { empty }
    }

    /**
     * Export the snapshot as a plain JSON string suitable for clipboard
     * transport between apps. No encryption — by design, so the user can
     * inspect / paste it anywhere.
     */
    fun exportSessionAsJsonString(snapshot: JsonObject): String = gson.toJson(snapshot)

    /**
     * Parse a pasted session JSON string into a snapshot. Returns null if
     * the input is not valid JSON or not a JSON object.
     */
    fun importSessionFromJsonString(text: String): JsonObject? {
        return try {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return null
            val el = JsonParser.parseString(trimmed)
            if (el.isJsonObject) el.asJsonObject else null
        } catch (_: Exception) { null }
    }

    /**
     * Export session to an AES-256-GCM encrypted file.
     * Returns the exported file path on success, null on failure.
     */
    fun exportSession(snapshot: JsonObject): String? {
        return try {
            val plaintext = gson.toJson(snapshot).toByteArray(Charsets.UTF_8)
            val encrypted = encrypt(plaintext)
            val exportFile = File(sessionDir, "session_export_${System.currentTimeMillis()}.waos")
            exportFile.writeBytes(encrypted)
            exportFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Export the latest session snapshot to a specific file path.
     */
    fun exportSessionToFile(snapshot: JsonObject, targetPath: String): Boolean {
        return try {
            val plaintext = gson.toJson(snapshot).toByteArray(Charsets.UTF_8)
            val encrypted = encrypt(plaintext)
            File(targetPath).writeBytes(encrypted)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // -------------------------------------------------------------------------
    // SESSION IMPORT
    // -------------------------------------------------------------------------

    /**
     * Import session from an encrypted .waos file.
     * Returns the decrypted JsonObject on success, null on failure.
     */
    fun importSession(sourcePath: String): JsonObject? {
        return try {
            val encrypted = File(sourcePath).readBytes()
            val plaintext = decrypt(encrypted)
            JsonParser.parseString(String(plaintext, Charsets.UTF_8)).asJsonObject
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Apply an imported session snapshot:
     * 1. Inject cookies via CookieManager (before loading URL)
     * 2. Return JS snippets to inject localStorage + sessionStorage after page load
     */
    fun applySessionSnapshot(snapshot: JsonObject): SessionRestoreData {
        val url = snapshot.get("url")?.asString ?: ""

        val cookiesArray = snapshot.getAsJsonArray("cookies")
        val cookieList: List<Map<String, String>> = if (cookiesArray != null) {
            cookiesArray.map { element ->
                val obj = element.asJsonObject
                obj.keySet().associateWith { key -> obj.get(key).asString }
            }
        } else emptyList()

        injectCookies(cookieList)

        val localStorageJson = snapshot.get("localStorage")?.toString() ?: "{}"
        val sessionStorageJson = snapshot.get("sessionStorage")?.toString() ?: "{}"
        val userAgent = snapshot.get("userAgent")?.asString ?: ""

        return SessionRestoreData(
            url = url,
            userAgent = userAgent,
            localStorageRestoreJs = buildLocalStorageRestoreJs(localStorageJson),
            sessionStorageRestoreJs = buildSessionStorageRestoreJs(sessionStorageJson)
        )
    }

    // -------------------------------------------------------------------------
    // PERSISTENCE (last known session for auto-restore)
    // -------------------------------------------------------------------------

    fun saveLastSessionSnapshot(snapshot: JsonObject) {
        try {
            val plaintext = gson.toJson(snapshot).toByteArray(Charsets.UTF_8)
            val encrypted = encrypt(plaintext)
            File(sessionDir, "last_session.waos").writeBytes(encrypted)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadLastSessionSnapshot(): JsonObject? {
        return try {
            val file = File(sessionDir, "last_session.waos")
            if (!file.exists()) return null
            val plaintext = decrypt(file.readBytes())
            JsonParser.parseString(String(plaintext, Charsets.UTF_8)).asJsonObject
        } catch (e: Exception) {
            null
        }
    }

    fun clearSessionData() {
        sessionDir.deleteRecursively()
        sessionDir.mkdirs()
    }

    fun getSessionSize(): Long = sessionDir.walkTopDown().sumOf { it.length() }

    // -------------------------------------------------------------------------
    // AES-256-GCM ENCRYPTION (Android Keystore)
    // -------------------------------------------------------------------------

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        if (keyStore.containsAlias(keyAlias)) {
            return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    private fun encrypt(plaintext: ByteArray): ByteArray {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        // Prepend IV length (1 byte) + IV + ciphertext
        return byteArrayOf(iv.size.toByte()) + iv + ciphertext
    }

    private fun decrypt(data: ByteArray): ByteArray {
        val secretKey = getOrCreateSecretKey()
        val ivLength = data[0].toInt() and 0xFF
        val iv = data.copyOfRange(1, 1 + ivLength)
        val ciphertext = data.copyOfRange(1 + ivLength, data.size)
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private fun extractDomain(url: String): String {
        return try {
            val host = java.net.URL(url).host
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            url
        }
    }
}

data class SessionRestoreData(
    val url: String,
    val userAgent: String,
    val localStorageRestoreJs: String,
    val sessionStorageRestoreJs: String
)
