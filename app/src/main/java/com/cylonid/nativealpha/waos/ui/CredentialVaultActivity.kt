package com.cylonid.nativealpha.waos.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.helper.BiometricPromptHelper
import com.cylonid.nativealpha.waos.model.CredentialItem
import com.cylonid.nativealpha.waos.model.CredentialRepository
import com.cylonid.nativealpha.waos.util.WaosConstants
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class CredentialVaultActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_EXPORT_VAULT = 4132
        private const val REQUEST_IMPORT_VAULT = 4133
        private const val DEFAULT_TIMEOUT_MINUTES = 5
    }

    private lateinit var credentialRecyclerView: RecyclerView
    private lateinit var adapter: CredentialAdapter
    private var appId: Int = -1
    private var vaultPin: String? = null
    private val autoLockHandler = Handler(Looper.getMainLooper())
    private var autoLockTimeoutMs = (DEFAULT_TIMEOUT_MINUTES * 60 * 1000).toLong()
    private val autoLockRunnable = Runnable {
        vaultPin = null
        Toast.makeText(this, "Vault locked due to inactivity", Toast.LENGTH_SHORT).show()
        ensurePinUnlocked()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credential_vault)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Credential Vault"

        appId = intent.getIntExtra(WaosConstants.EXTRA_WAOS_APP_ID, -1)
        credentialRecyclerView = findViewById(R.id.credential_recycler_view)
        credentialRecyclerView.layoutManager = LinearLayoutManager(this)

        val prefs = getSharedPreferences("waos_app_settings", Context.MODE_PRIVATE)
        val timeoutMinutes = prefs.getInt("${appId}_credential_timeout_min", DEFAULT_TIMEOUT_MINUTES)
        autoLockTimeoutMs = (timeoutMinutes * 60 * 1000).toLong()

        findViewById<Button>(R.id.button_add_credential).setOnClickListener {
            hapticTap()
            showAddCredentialDialog()
        }
        findViewById<Button>(R.id.button_export_vault).setOnClickListener {
            hapticTap()
            exportVault()
        }
        findViewById<Button>(R.id.button_import_vault).setOnClickListener {
            hapticTap()
            importVault()
        }

        ensurePinUnlocked()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetAutoLockTimer()
    }

    private fun startAutoLockTimer() {
        autoLockHandler.removeCallbacks(autoLockRunnable)
        if (autoLockTimeoutMs > 0) {
            autoLockHandler.postDelayed(autoLockRunnable, autoLockTimeoutMs)
        }
    }

    private fun resetAutoLockTimer() {
        if (vaultPin != null) {
            autoLockHandler.removeCallbacks(autoLockRunnable)
            autoLockHandler.postDelayed(autoLockRunnable, autoLockTimeoutMs)
        }
    }

    private fun cancelAutoLockTimer() {
        autoLockHandler.removeCallbacks(autoLockRunnable)
    }

    private fun hapticTap() {
        try {
            val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(30)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAutoLockTimer()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (vaultPin != null) {
            startAutoLockTimer()
        }
    }

    private fun ensurePinUnlocked() {
        val prefs = getSharedPreferences("waos_vault", Context.MODE_PRIVATE)
        val storedHash = prefs.getString("vault_pin_hash", null)
        val biometricEnabled = prefs.getBoolean("vault_biometric_enabled", false)
        if (storedHash == null) {
            showSetPinDialog()
        } else if (biometricEnabled) {
            BiometricPromptHelper(this).showPrompt(
                {
                    val savedPin = prefs.getString("vault_pin_secret", null)
                    if (savedPin != null) {
                        vaultPin = savedPin
                        refreshCredentials()
                    } else {
                        showUnlockDialog(storedHash)
                    }
                },
                { showUnlockDialog(storedHash) },
                getString(R.string.bioprompt_restricted_webapp)
            )
        } else {
            showUnlockDialog(storedHash)
        }
    }

    private fun showSetPinDialog() {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_set_pin, null)
        val pinInput = view.findViewById<EditText>(R.id.edit_pin)
        val confirmInput = view.findViewById<EditText>(R.id.edit_pin_confirm)
        builder.setTitle("Set Credential Vault PIN")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val pin = pinInput.text.toString().trim()
                val confirm = confirmInput.text.toString().trim()
                if (pin.length >= 4 && pin == confirm) {
                    val hash = pin.hashCode().toString()
                    getSharedPreferences("waos_vault", Context.MODE_PRIVATE)
                        .edit().putString("vault_pin_hash", hash).apply()
                    vaultPin = pin
                    showBiometricEnablePrompt()
                } else {
                    Toast.makeText(this, "PINs must match and have at least 4 digits", Toast.LENGTH_SHORT).show()
                    showSetPinDialog()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun showBiometricEnablePrompt() {
        BiometricPromptHelper(this).showPrompt(
            {
                getSharedPreferences("waos_vault", Context.MODE_PRIVATE)
                    .edit().putBoolean("vault_biometric_enabled", true)
                    .putString("vault_pin_secret", vaultPin)
                    .apply()
                refreshCredentials()
            },
            {
                getSharedPreferences("waos_vault", Context.MODE_PRIVATE)
                    .edit().putBoolean("vault_biometric_enabled", false)
                    .remove("vault_pin_secret")
                    .apply()
                refreshCredentials()
            },
            "Enable biometric vault unlock?"
        )
    }

    private fun showUnlockDialog(storedHash: String) {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_unlock_pin, null)
        val pinInput = view.findViewById<EditText>(R.id.edit_pin)
        builder.setTitle("Unlock Vault")
            .setView(view)
            .setPositiveButton("Unlock") { _, _ ->
                val pin = pinInput.text.toString().trim()
                if (pin.hashCode().toString() == storedHash) {
                    vaultPin = pin
                    refreshCredentials()
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    showUnlockDialog(storedHash)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun refreshCredentials() {
        val pin = vaultPin ?: return
        val credentials = CredentialRepository.loadCredentials(this, appId, pin)
        adapter = CredentialAdapter(credentials) { item -> showCredentialActions(item) }
        credentialRecyclerView.adapter = adapter
        startAutoLockTimer()
    }

    private fun showCredentialActions(item: CredentialItem) {
        AlertDialog.Builder(this)
            .setTitle(item.title)
            .setItems(arrayOf("Autofill in browser", "Copy username", "Copy password", "Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> autofillCredential(item)
                    1 -> copyTextToClipboard(item.username)
                    2 -> copyTextToClipboard(item.password)
                    3 -> showEditCredentialDialog(item)
                    4 -> confirmDeleteCredential(item)
                }
            }
            .show()
    }

    private fun showEditCredentialDialog(item: CredentialItem) {
        if (vaultPin == null) return
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_credential, null)
        val titleInput = view.findViewById<EditText>(R.id.edit_title)
        val usernameInput = view.findViewById<EditText>(R.id.edit_username)
        val passwordInput = view.findViewById<EditText>(R.id.edit_password)
        val urlInput = view.findViewById<EditText>(R.id.edit_url)
        val notesInput = view.findViewById<EditText>(R.id.edit_notes)

        titleInput.setText(item.title)
        usernameInput.setText(item.username)
        passwordInput.setText(item.password)
        urlInput.setText(item.url)
        notesInput.setText(item.notes)

        val dialog = builder.setTitle("Edit Credential")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val updated = CredentialItem(
                    appId,
                    titleInput.text.toString().trim(),
                    usernameInput.text.toString().trim(),
                    passwordInput.text.toString().trim(),
                    urlInput.text.toString().trim(),
                    notesInput.text.toString().trim(),
                    item.timestamp
                )
                CredentialRepository.updateCredential(this, updated, vaultPin!!)
                refreshCredentials()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Generate", null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            passwordInput.setText(generatePassword())
        }
    }

    private fun confirmDeleteCredential(item: CredentialItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Credential")
            .setMessage("Delete ${item.title}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                CredentialRepository.deleteCredential(this, item)
                refreshCredentials()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun copyCredential(item: CredentialItem, addLabel: Boolean) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val payload = if (addLabel) "${item.username}:${item.password}" else "${item.username}:${item.password}"
        val clip = ClipData.newPlainText("WAOS Credential", payload)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Credential copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun copyTextToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WAOS Credential", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun autofillCredential(item: CredentialItem) {
        val intent = Intent()
        intent.putExtra(WaosConstants.EXTRA_CREDENTIAL_USERNAME, item.username)
        intent.putExtra(WaosConstants.EXTRA_CREDENTIAL_PASSWORD, item.password)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun showAddCredentialDialog() {
        if (vaultPin == null) return
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_credential, null)
        val titleInput = view.findViewById<EditText>(R.id.edit_title)
        val usernameInput = view.findViewById<EditText>(R.id.edit_username)
        val passwordInput = view.findViewById<EditText>(R.id.edit_password)
        val urlInput = view.findViewById<EditText>(R.id.edit_url)
        val notesInput = view.findViewById<EditText>(R.id.edit_notes)
        val dialog = builder.setTitle("Add Credential")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val credential = CredentialItem(
                    appId,
                    titleInput.text.toString().trim(),
                    usernameInput.text.toString().trim(),
                    passwordInput.text.toString().trim(),
                    urlInput.text.toString().trim(),
                    notesInput.text.toString().trim()
                )
                CredentialRepository.saveCredential(this, credential, vaultPin!!)
                refreshCredentials()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Generate", null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            passwordInput.setText(generatePassword())
        }
    }

    private fun generatePassword(): String {
        return UUID.randomUUID().toString().replace("-", "").take(16)
    }

    private fun exportVault() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, "waos_vault_export.json")
        }
        startActivityForResult(intent, REQUEST_EXPORT_VAULT)
    }

    private fun importVault() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType("application/json")
            .addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, REQUEST_IMPORT_VAULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return
        when (requestCode) {
            REQUEST_EXPORT_VAULT -> {
                val uri = data.data ?: return
                exportVaultFile(uri)
            }
            REQUEST_IMPORT_VAULT -> {
                val uri = data.data ?: return
                importVaultFile(uri)
            }
        }
    }

    private fun exportVaultFile(uri: Uri) {
        try {
            val file = File(filesDir, "waos_credentials.json")
            if (!file.exists()) throw Exception("Vault file not found")
            contentResolver.openOutputStream(uri).use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output!!)
                }
            }
            Toast.makeText(this, "Vault exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not export vault", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun importVaultFile(uri: Uri) {
        try {
            val target = File(filesDir, "waos_credentials.json")
            contentResolver.openInputStream(uri).use { input ->
                FileOutputStream(target).use { output ->
                    input?.copyTo(output)
                }
            }
            refreshCredentials()
            Toast.makeText(this, "Vault imported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not import vault", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
