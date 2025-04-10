package com.cylonid.nativealpha

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.cylonid.nativealpha.databinding.WebappSettingsBinding
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.util.Const
import com.cylonid.nativealpha.util.DateUtils.convertStringToCalendar
import com.cylonid.nativealpha.util.DateUtils.getHourMinFormat
import com.cylonid.nativealpha.util.ProcessUtils.closeAllWebAppsAndProcesses
import com.cylonid.nativealpha.util.Utility
import java.util.Calendar

class WebAppSettingsActivity : AppCompatActivity() {
    var webappID: Int = -1
    var webapp: WebApp? = null
    var isGlobalWebApp: Boolean = false

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<WebappSettingsBinding>(
            this, R.layout.webapp_settings
        )
        val txt = findViewById<TextView>(R.id.txthintUserAgent)
        txt.text = Html.fromHtml(getString(R.string.hint_user_agent), Html.FROM_HTML_MODE_LEGACY)
        txt.movementMethod = LinkMovementMethod.getInstance()

        webappID = intent.getIntExtra(Const.INTENT_WEBAPPID, -1)
        Utility.Assert(webappID != -1, "WebApp ID could not be retrieved.")
        isGlobalWebApp = webappID == DataManager.getInstance().settings.globalWebApp.ID

        val inflated_view = binding.root

        if (isGlobalWebApp) {
            webapp = DataManager.getInstance().settings.globalWebApp
            prepareGlobalWebAppScreen()
        } else webapp = DataManager.getInstance().getWebAppIgnoringGlobalOverride(webappID, true)

        if (webapp == null) {
            finish()
        } else {
            val modified_webapp = WebApp(webapp!!)
            binding.webapp = modified_webapp
            binding.activity = this@WebAppSettingsActivity

            val btnCreateShortcut = inflated_view.findViewById<Button>(R.id.btnRecreateShortcut)

            btnCreateShortcut.setOnClickListener { view: View? ->
                val frag = ShortcutDialogFragment.newInstance(webapp)
                frag.show(supportFragmentManager, "SCFetcher-" + webapp!!.ID)
            }
            val btnSave = findViewById<Button>(R.id.btnSave)
            val btnCancel = findViewById<Button>(R.id.btnCancel)

            btnSave.setOnClickListener { v: View? ->
                val activityManager =
                    getSystemService(ACTIVITY_SERVICE) as ActivityManager
                //Global web app => close all webview activities, save to global settings
                if (isGlobalWebApp) {
                    closeAllWebAppsAndProcesses(
                        activityManager
                    )
                    DataManager.getInstance().settings.globalWebApp = modified_webapp
                    DataManager.getInstance().saveGlobalSettings()
                } else {
                    for (task in activityManager.appTasks) {
                        val id = task.taskInfo.baseIntent.getIntExtra(
                            Const.INTENT_WEBAPPID,
                            -1
                        )
                        if (id == webappID) task.finishAndRemoveTask()
                    }
                    for (processInfo in activityManager.runningAppProcesses) {
                        if (processInfo.processName.contains("web_sandbox_" + modified_webapp.containerId)) {
                            Process.killProcess(processInfo.pid)
                        }
                    }
                    DataManager.getInstance().replaceWebApp(modified_webapp)
                }

                val i = Intent(this@WebAppSettingsActivity, MainActivity::class.java)
                i.putExtra(Const.INTENT_WEBAPP_CHANGED, true)
                finish()
                startActivity(i)
            }

            btnCancel.setOnClickListener { v: View? -> finish() }
            val txtBeginDarkMode = inflated_view.findViewById<EditText>(R.id.textDarkModeBegin)
            val txtEndDarkMode = inflated_view.findViewById<EditText>(R.id.textDarkModeEnd)

            txtBeginDarkMode.setOnClickListener { view: View? ->
                showTimePicker(
                    txtBeginDarkMode
                )
            }
            txtEndDarkMode.setOnClickListener { view: View? ->
                showTimePicker(
                    txtEndDarkMode
                )
            }

            webapp!!.onSwitchExpertSettingsChanged(
                inflated_view.findViewById(R.id.switchExpertSettings),
                webapp!!.isShowExpertSettings
            )
            webapp!!.onSwitchOverrideGlobalSettingsChanged(
                findViewById(R.id.switchOverrideGlobal),
                webapp!!.isOverrideGlobalSettings
            )
            setPlusSettings(inflated_view)
        }
    }

    private fun setPlusSettings(v: View) {
        val secDarkMode = v.findViewById<LinearLayout>(R.id.sectionDarkmode)
        val secSandbox = v.findViewById<LinearLayout>(R.id.sectionSandbox)
        val secKiosk = v.findViewById<LinearLayout>(R.id.sectionKioskMode)
        val secAccessRestriction = v.findViewById<LinearLayout>(R.id.sectionAccessRestriction)
        if (BuildConfig.FLAVOR != "extended") {
            secDarkMode.visibility = View.GONE
            secSandbox.visibility = View.GONE
            secKiosk.visibility = View.GONE
            secAccessRestriction.visibility = View.GONE
        }
    }


    private fun showTimePicker(txtField: EditText) {
        val c = convertStringToCalendar(txtField.text.toString())
        val timePickerDialog = TimePickerDialog(
            this@WebAppSettingsActivity, R.style.AppTheme,
            { timePicker: TimePicker?, selectedHour: Int, selectedMinute: Int ->
                val datetime = Calendar.getInstance()
                datetime[Calendar.HOUR_OF_DAY] = selectedHour
                datetime[Calendar.MINUTE] = selectedMinute
                txtField.setText(getHourMinFormat().format(datetime.time))
            }, c!![Calendar.HOUR_OF_DAY], c[Calendar.MINUTE], true
        )
        timePickerDialog.show()
    }

    private fun prepareGlobalWebAppScreen() {
        findViewById<View>(R.id.btnRecreateShortcut).visibility =
            View.GONE
        findViewById<View>(R.id.labelWebAppName).visibility =
            View.GONE
        findViewById<View>(R.id.txtWebAppName).visibility =
            View.GONE
        findViewById<View>(R.id.switchOverrideGlobal).visibility =
            View.GONE
        findViewById<View>(R.id.sectionSSL).visibility =
            View.GONE
        findViewById<View>(R.id.sectionSandbox).visibility =
            View.GONE
        findViewById<View>(R.id.labelTitle).visibility =
            View.GONE
        findViewById<View>(R.id.labelEditableBaseUrl).visibility =
            View.GONE
        findViewById<View>(R.id.textBaseUrl).visibility =
            View.GONE

        val page_title = findViewById<TextView>(R.id.labelPageTitle)
        page_title.text = getString(R.string.global_web_app_settings)
    }
}


