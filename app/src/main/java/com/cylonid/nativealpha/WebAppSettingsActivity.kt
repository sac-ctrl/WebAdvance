package com.cylonid.nativealpha

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.cylonid.nativealpha.activities.ToolbarBaseActivity
import com.cylonid.nativealpha.databinding.WebappSettingsBinding
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.util.Const
import com.cylonid.nativealpha.util.DateUtils.convertStringToCalendar
import com.cylonid.nativealpha.util.DateUtils.getHourMinFormat
import com.cylonid.nativealpha.util.ProcessUtils.closeAllWebAppsAndProcesses
import com.cylonid.nativealpha.util.Utility
import java.util.Calendar
import kotlin.concurrent.thread

class WebAppSettingsActivity : ToolbarBaseActivity<WebappSettingsBinding>() {
    var webappID: Int = -1
    var webapp: WebApp? = null
    private var isGlobalWebApp: Boolean = false

    private lateinit var waosGroupInput: EditText
    private lateinit var waosIconInput: EditText
    private lateinit var waosIconPickerButton: Button
    private lateinit var waosFaviconFetchButton: Button
    private lateinit var waosDownloadFolderInput: EditText
    private lateinit var waosClipboardMaxInput: EditText
    private lateinit var waosFloatingWidthInput: EditText
    private lateinit var waosFloatingHeightInput: EditText
    private lateinit var waosFloatingOpacityInput: EditText
    private lateinit var waosCacheModeButton: Button
    private lateinit var waosLinkFormatButton: Button
    private lateinit var waosCredentialTimeoutInput: EditText
    private lateinit var waosClipboardSyncSwitch: com.google.android.material.materialswitch.MaterialSwitch

    private val REQUEST_CODE_ICON_PICKER = 5150

    private val cacheModes = arrayOf("Default", "No Cache", "Cache Only", "Cache Else Network", "No Store")
    private val cacheModeValues = arrayOf("default", "no_cache", "cache_only", "cache_else_network", "no_store")

    private val linkFormats = arrayOf("Plain URL", "URL + Title", "Markdown", "HTML Link", "BBCode")
    private val linkFormatValues = arrayOf("url", "url_title", "markdown", "html", "bbcode")

    private var selectedCacheMode = "default"
    private var selectedLinkFormat = "url"

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setToolbarTitle(getString(R.string.web_app_settings))

        webappID = intent.getIntExtra(Const.INTENT_WEBAPPID, -1)
        Utility.Assert(webappID != -1, "WebApp ID could not be retrieved.")
        isGlobalWebApp = webappID == DataManager.getInstance().settings.globalWebApp.ID

        if (isGlobalWebApp) {
            webapp = DataManager.getInstance().settings.globalWebApp
            prepareGlobalWebAppScreen()
        } else webapp = DataManager.getInstance().getWebAppIgnoringGlobalOverride(webappID, true)

        if (webapp == null) {
            finish()
            return
        }
        val modifiedWebapp = WebApp(webapp!!)
        binding.webapp = modifiedWebapp
        binding.activity = this@WebAppSettingsActivity
        binding.lifecycleOwner = this

        setupSaveAndCancel(modifiedWebapp)
        setupDarkModeElements()
        setupPlusSettings()
        setupDesktopUserAgentHint()
        setupShortcutButton()
        setupSwitchListeners(webapp!!)
        setupWaosAdvancedSettings(modifiedWebapp)
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): WebappSettingsBinding {
        return WebappSettingsBinding.inflate(layoutInflater)
    }

    private fun setupSwitchListeners(webapp: WebApp) {
        webapp.onSwitchExpertSettingsChanged(
            binding.switchExpertSettings,
            webapp.isShowExpertSettings
        )
        webapp.onSwitchOverrideGlobalSettingsChanged(
            binding.switchOverrideGlobal,
            webapp.isOverrideGlobalSettings
        )
    }

    private fun setupWaosAdvancedSettings(modifiedWebapp: WebApp) {
        waosGroupInput = findViewById(R.id.textGroup)
        waosIconInput = findViewById(R.id.textIconUri)
        waosIconPickerButton = findViewById(R.id.button_select_icon)
        waosFaviconFetchButton = findViewById(R.id.button_fetch_favicon)
        waosDownloadFolderInput = findViewById(R.id.textDownloadFolder)
        waosClipboardMaxInput = findViewById(R.id.textClipboardMaxItems)
        waosFloatingWidthInput = findViewById(R.id.textFloatingWidth)
        waosFloatingHeightInput = findViewById(R.id.textFloatingHeight)
        waosFloatingOpacityInput = findViewById(R.id.textFloatingOpacity)
        waosCacheModeButton = findViewById(R.id.button_cache_mode)
        waosLinkFormatButton = findViewById(R.id.button_link_format)
        waosCredentialTimeoutInput = findViewById(R.id.textCredentialTimeout)
        waosClipboardSyncSwitch = findViewById(R.id.switchClipboardSync)

        waosGroupInput.setText(modifiedWebapp.group)
        waosIconInput.setText(modifiedWebapp.iconUri ?: "")
        waosDownloadFolderInput.setText(modifiedWebapp.customDownloadFolder ?: "")
        waosClipboardMaxInput.setText(modifiedWebapp.clipboardMaxItems.toString())
        waosFloatingWidthInput.setText(modifiedWebapp.floatingWindowWidth.toString())
        waosFloatingHeightInput.setText(modifiedWebapp.floatingWindowHeight.toString())
        waosFloatingOpacityInput.setText(modifiedWebapp.floatingWindowOpacity.toString())
        waosClipboardSyncSwitch.isChecked = modifiedWebapp.clipboardSyncEnabled

        val prefs = getSharedPreferences("waos_app_settings", Context.MODE_PRIVATE)
        selectedCacheMode = prefs.getString("${webappID}_cache_mode", "default") ?: "default"
        selectedLinkFormat = prefs.getString("${webappID}_link_format", "url") ?: "url"
        val timeoutMinutes = prefs.getInt("${webappID}_credential_timeout_min", 5)

        updateCacheModeButton()
        updateLinkFormatButton()
        waosCredentialTimeoutInput.setText(timeoutMinutes.toString())

        waosIconPickerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_CODE_ICON_PICKER)
        }

        waosFaviconFetchButton.setOnClickListener {
            fetchFavicon(modifiedWebapp.baseUrl)
        }

        waosCacheModeButton.setOnClickListener {
            showCacheModeSelector()
        }

        waosLinkFormatButton.setOnClickListener {
            showLinkFormatSelector()
        }
    }

    private fun updateCacheModeButton() {
        val index = cacheModeValues.indexOf(selectedCacheMode).coerceAtLeast(0)
        waosCacheModeButton.text = "Cache Mode: ${cacheModes[index]}"
    }

    private fun updateLinkFormatButton() {
        val index = linkFormatValues.indexOf(selectedLinkFormat).coerceAtLeast(0)
        waosLinkFormatButton.text = "Link Format: ${linkFormats[index]}"
    }

    private fun showCacheModeSelector() {
        val currentIndex = cacheModeValues.indexOf(selectedCacheMode).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Select Cache Mode")
            .setSingleChoiceItems(cacheModes, currentIndex) { dialog, which ->
                selectedCacheMode = cacheModeValues[which]
                updateCacheModeButton()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLinkFormatSelector() {
        val currentIndex = linkFormatValues.indexOf(selectedLinkFormat).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Select Link Copy Format")
            .setSingleChoiceItems(linkFormats, currentIndex) { dialog, which ->
                selectedLinkFormat = linkFormatValues[which]
                updateLinkFormatButton()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchFavicon(baseUrl: String) {
        if (baseUrl.isBlank()) {
            Toast.makeText(this, "No URL to fetch favicon from", Toast.LENGTH_SHORT).show()
            return
        }
        waosFaviconFetchButton.isEnabled = false
        waosFaviconFetchButton.text = "…"
        Toast.makeText(this, "Fetching favicon…", Toast.LENGTH_SHORT).show()

        thread {
            try {
                val url = if (baseUrl.startsWith("http")) baseUrl else "https://$baseUrl"
                val domain = java.net.URL(url).host
                val faviconUrl = "https://www.google.com/s2/favicons?sz=64&domain=$domain"
                val faviconHighRes = "https://icons.duckduckgo.com/ip3/$domain.ico"
                val googleFavicon = "https://www.google.com/s2/favicons?domain=$domain"

                Handler(Looper.getMainLooper()).post {
                    waosIconInput.setText(faviconHighRes)
                    waosFaviconFetchButton.isEnabled = true
                    waosFaviconFetchButton.text = "Fetch"
                    Snackbar.make(binding.root, "Favicon URL fetched! Tap Save to apply.", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    waosFaviconFetchButton.isEnabled = true
                    waosFaviconFetchButton.text = "Fetch"
                    Toast.makeText(this, "Could not fetch favicon: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSaveAndCancel(modifiedWebapp: WebApp) {
        binding.btnSave.setOnClickListener {
            modifiedWebapp.group = waosGroupInput.text.toString().trim().ifBlank { "Default" }
            modifiedWebapp.iconUri = waosIconInput.text.toString().trim().ifEmpty { null }
            modifiedWebapp.customDownloadFolder = waosDownloadFolderInput.text.toString().trim().ifEmpty { null }
            modifiedWebapp.clipboardMaxItems = waosClipboardMaxInput.text.toString().toIntOrNull()?.coerceIn(1, 500) ?: modifiedWebapp.clipboardMaxItems
            modifiedWebapp.floatingWindowWidth = waosFloatingWidthInput.text.toString().toIntOrNull() ?: modifiedWebapp.floatingWindowWidth
            modifiedWebapp.floatingWindowHeight = waosFloatingHeightInput.text.toString().toIntOrNull() ?: modifiedWebapp.floatingWindowHeight
            modifiedWebapp.floatingWindowOpacity = waosFloatingOpacityInput.text.toString().toIntOrNull()?.coerceIn(30, 100) ?: modifiedWebapp.floatingWindowOpacity
            modifiedWebapp.clipboardSyncEnabled = waosClipboardSyncSwitch.isChecked

            val timeoutMinutes = waosCredentialTimeoutInput.text.toString().toIntOrNull()?.coerceIn(1, 1440) ?: 5
            val prefs = getSharedPreferences("waos_app_settings", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("${webappID}_cache_mode", selectedCacheMode)
                .putString("${webappID}_link_format", selectedLinkFormat)
                .putInt("${webappID}_credential_timeout_min", timeoutMinutes)
                .apply()

            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            if (isGlobalWebApp) {
                closeAllWebAppsAndProcesses(activityManager)
                DataManager.getInstance().settings.globalWebApp = modifiedWebapp
                DataManager.getInstance().saveGlobalSettings()
            } else {
                for (task in activityManager.appTasks) {
                    val id = task.taskInfo.baseIntent.getIntExtra(Const.INTENT_WEBAPPID, -1)
                    if (id == webappID) task.finishAndRemoveTask()
                }
                for (processInfo in activityManager.runningAppProcesses) {
                    if (processInfo.processName.contains("web_sandbox_" + modifiedWebapp.containerId)) {
                        Process.killProcess(processInfo.pid)
                    }
                }
                DataManager.getInstance().replaceWebApp(modifiedWebapp)
            }

            val i = Intent(this@WebAppSettingsActivity, MainActivity::class.java)
            i.putExtra(Const.INTENT_WEBAPP_CHANGED, true)
            finish()
            startActivity(i)
        }

        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun setupDarkModeElements() {
        val txtBeginDarkMode = binding.textDarkModeBegin
        val txtEndDarkMode = binding.textDarkModeEnd
        txtBeginDarkMode.setOnClickListener { showTimePicker(txtBeginDarkMode) }
        txtEndDarkMode.setOnClickListener { showTimePicker(txtEndDarkMode) }
    }

    private fun setupDesktopUserAgentHint() {
        val txt = binding.txthintUserAgent
        txt.text = Html.fromHtml(getString(R.string.hint_user_agent), Html.FROM_HTML_MODE_LEGACY)
        txt.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupShortcutButton() {
        binding.btnRecreateShortcut.setOnClickListener {
            val frag = ShortcutDialogFragment.newInstance(webapp)
            frag.show(supportFragmentManager, "SCFetcher-" + webapp!!.ID)
        }
    }

    private fun setupPlusSettings() {
        if (!BuildConfig.FLAVOR.contains("extended")) {
            binding.sectionDarkmode.visibility = View.GONE
            binding.sectionSandbox.visibility = View.GONE
            binding.sectionKioskMode.visibility = View.GONE
            binding.sectionAccessRestriction.visibility = View.GONE
        }
    }

    private fun showTimePicker(txtField: EditText) {
        val c = convertStringToCalendar(txtField.text.toString())
        val timePickerDialog = TimePickerDialog(
            this@WebAppSettingsActivity, R.style.AppTheme,
            { _: TimePicker?, selectedHour: Int, selectedMinute: Int ->
                val datetime = Calendar.getInstance()
                datetime[Calendar.HOUR_OF_DAY] = selectedHour
                datetime[Calendar.MINUTE] = selectedMinute
                txtField.setText(getHourMinFormat().format(datetime.time))
            }, c!![Calendar.HOUR_OF_DAY], c[Calendar.MINUTE], true
        )
        timePickerDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ICON_PICKER && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                waosIconInput.setText(uri.toString())
                Snackbar.make(binding.root, getString(R.string.icon_selected), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareGlobalWebAppScreen() {
        binding.btnRecreateShortcut.visibility = View.GONE
        binding.labelWebAppName.visibility = View.GONE
        binding.txtWebAppName.visibility = View.GONE
        binding.switchOverrideGlobal.visibility = View.GONE
        binding.sectionSSL.visibility = View.GONE
        binding.sectionSandbox.visibility = View.GONE
        binding.labelTitle.visibility = View.GONE
        binding.labelEditableBaseUrl.visibility = View.GONE
        binding.textBaseUrl.visibility = View.GONE
        binding.globalSettingsInfoText.visibility = View.VISIBLE
        setToolbarTitle(getString(R.string.global_web_app_settings))
    }
}
