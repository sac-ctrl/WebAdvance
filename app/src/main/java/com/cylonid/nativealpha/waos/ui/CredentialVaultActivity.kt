package com.cylonid.nativealpha.waos.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.model.CredentialItem
import com.cylonid.nativealpha.waos.model.CredentialRepository
import com.cylonid.nativealpha.waos.util.WaosConstants

class CredentialVaultActivity : AppCompatActivity() {
    private lateinit var credentialRecyclerView: RecyclerView
    private lateinit var adapter: CredentialAdapter
    private var appId: Int = -1
    private var vaultPin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credential_vault)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appId = intent.getIntExtra(WaosConstants.EXTRA_WAOS_APP_ID, -1)
        credentialRecyclerView = findViewById(R.id.credential_recycler_view)
        credentialRecyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.button_add_credential).setOnClickListener {
            showAddCredentialDialog()
        }

        ensurePinUnlocked()
    }

    private fun ensurePinUnlocked() {
        val prefs = getSharedPreferences("waos_vault", Context.MODE_PRIVATE)
        val storedHash = prefs.getString("vault_pin_hash", null)
        if (storedHash == null) {
            showSetPinDialog()
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
                    refreshCredentials()
                } else {
                    Toast.makeText(this, "PINs must match and have at least 4 digits", Toast.LENGTH_SHORT).show()
                    showSetPinDialog()
                }
            }
            .setCancelable(false)
            .show()
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
        adapter = CredentialAdapter(credentials) { item -> copyCredential(item) }
        credentialRecyclerView.adapter = adapter
    }

    private fun copyCredential(item: CredentialItem) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WAOS Credential", "${item.username}:${item.password}")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Credential copied to clipboard", Toast.LENGTH_SHORT).show()
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
        builder.setTitle("Add Credential")
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
            .show()
    }
}
