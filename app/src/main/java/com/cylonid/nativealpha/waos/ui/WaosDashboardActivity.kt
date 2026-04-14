package com.cylonid.nativealpha.waos.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cylonid.nativealpha.R
import java.util.Collections
import com.cylonid.nativealpha.WebAppSettingsActivity
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.util.Const
import com.cylonid.nativealpha.util.WebViewLauncher
import com.cylonid.nativealpha.waos.util.WaosConstants
import com.google.android.material.floatingactionbutton.FloatingActionButton

class WaosDashboardActivity : AppCompatActivity() {
    private lateinit var dashboardRecyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var appCountText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var toggleLayoutButton: Button
    private lateinit var sortAppsButton: Button
    private lateinit var addAppButton: FloatingActionButton
    private lateinit var adapter: WaosAppAdapter
    private var isGridMode = true
    private var currentSortMode = SortMode.ORDER

    private enum class SortMode {
        ORDER,
        NAME,
        GROUP
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waos_dashboard)

        DataManager.getInstance().loadAppData()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appCountText = findViewById(R.id.waos_app_count)
        searchInput = findViewById(R.id.search_input)
        toggleLayoutButton = findViewById(R.id.button_toggle_layout)
        sortAppsButton = findViewById(R.id.button_sort_apps)
        addAppButton = findViewById(R.id.button_add_app)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        dashboardRecyclerView = findViewById(R.id.dashboard_recycler_view)
        dashboardRecyclerView.layoutManager = GridLayoutManager(this, 2)
        dashboardRecyclerView.itemAnimator = DefaultItemAnimator()

        adapter = WaosAppAdapter(
            emptyList(),
            onOpenApp = { app -> openWebApp(app) },
            onShowDownloads = { app -> openDownloadHistory(app.ID) },
            onShowClipboard = { app -> openClipboard(app.ID) },
            onShowCredentials = { app -> openCredentials(app.ID) },
            onShowOptions = { app -> showAppActions(app) }
        )
        dashboardRecyclerView.adapter = adapter

        setupSearch()
        setupRefresh()
        setupLayoutToggle()
        setupSort()
        setupAddApp()
        setupDragAndDrop()
        loadApps()

        val launchBubbleButton = findViewById<Button>(R.id.button_launch_floating)
        launchBubbleButton.setOnClickListener {
            startService(Intent(this, com.cylonid.nativealpha.waos.service.FloatingWindowService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun loadApps() {
        val apps = when (currentSortMode) {
            SortMode.NAME -> DataManager.getInstance().getActiveWebsites().sortedBy { it.title.lowercase() }
            SortMode.GROUP -> DataManager.getInstance().getActiveWebsites().sortedWith(compareBy({ it.group.lowercase() }, { it.order }))
            SortMode.ORDER -> DataManager.getInstance().getActiveWebsites().sortedBy { it.order }
        }
        adapter.updateWebApps(apps)
        appCountText.text = getString(R.string.waos_app_count_format, apps.size)
        swipeRefreshLayout.isRefreshing = false
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSort() {
        sortAppsButton.setOnClickListener {
            currentSortMode = when (currentSortMode) {
                SortMode.ORDER -> SortMode.NAME
                SortMode.NAME -> SortMode.GROUP
                SortMode.GROUP -> SortMode.ORDER
            }
            sortAppsButton.text = when (currentSortMode) {
                SortMode.ORDER -> getString(R.string.sort_by_order)
                SortMode.NAME -> getString(R.string.sort_by_name)
                SortMode.GROUP -> getString(R.string.sort_by_group)
            }
            loadApps()
        }
        sortAppsButton.text = getString(R.string.sort_by_order)
    }

    private fun setupAddApp() {
        addAppButton.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.add_website_dialogue, null)
            val websiteUrl = view.findViewById<EditText>(R.id.websiteUrl)

            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.add_new_webapp)
                .setView(view)
                .setPositiveButton(R.string.add) { _, _ ->
                    val url = websiteUrl.text.toString().trim()
                    if (url.isNotBlank()) {
                        val normalizedUrl = if (url.startsWith("https://") || url.startsWith("http://")) url else "https://$url"
                        val newApp = WebApp(
                            normalizedUrl,
                            DataManager.getInstance().getIncrementedID(),
                            DataManager.getInstance().getIncrementedOrder()
                        )
                        newApp.applySettingsForNewWebApp()
                        DataManager.getInstance().addWebsite(newApp)
                        loadApps()
                        Toast.makeText(this, R.string.webapp_added, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()

            dialog.show()
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isEnabled = false
            websiteUrl.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    positive.isEnabled = !websiteUrl.text.isNullOrBlank()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun setupRefresh() {
        val refreshButton = findViewById<ImageButton>(R.id.button_refresh)
        refreshButton.setOnClickListener { loadApps() }
        swipeRefreshLayout.setOnRefreshListener { loadApps() }
    }

    private fun setupLayoutToggle() {
        toggleLayoutButton.text = if (isGridMode) "List" else "Grid"
        toggleLayoutButton.setOnClickListener {
            isGridMode = !isGridMode
            toggleLayoutButton.text = if (isGridMode) "List" else "Grid"
            dashboardRecyclerView.layoutManager = if (isGridMode) GridLayoutManager(this, 2) else LinearLayoutManager(this)
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupDragAndDrop() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                val updated = adapter.currentApps.toMutableList()
                Collections.swap(updated, from, to)
                adapter.updateWebApps(updated)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                applyOrderToApps(adapter.currentApps)
            }
        })
        itemTouchHelper.attachToRecyclerView(dashboardRecyclerView)
    }

    private fun applyOrderToApps(apps: List<WebApp>) {
        apps.forEachIndexed { index, webapp ->
            webapp.order = index + 1
            DataManager.getInstance().replaceWebApp(webapp)
        }
        loadApps()
    }

    private fun showAppActions(app: WebApp) {
        val lockLabel = if (app.isBiometricProtection) "Unlock" else "Lock"
        val actions = arrayOf("Open", "Settings", "Download History", "Clipboard", "Vault", "Clone", "Delete", lockLabel, "Copy URL", "Launch Floating Bubble")
        AlertDialog.Builder(this)
            .setTitle(app.getTitle())
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> openWebApp(app)
                    1 -> openAppSettings(app.ID)
                    2 -> openDownloadHistory(app.ID)
                    3 -> openClipboard(app.ID)
                    4 -> openCredentials(app.ID)
                    5 -> cloneWebApp(app)
                    6 -> confirmDeleteWebApp(app)
                    7 -> toggleLockWebApp(app)
                    8 -> copyAppUrl(app)
                    9 -> startService(Intent(this, com.cylonid.nativealpha.waos.service.FloatingWindowService::class.java))
                }
            }
            .show()
    }

    private fun copyAppUrl(app: WebApp) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("WAOS App URL", app.getBaseUrl()))
        Toast.makeText(this, "App URL copied", Toast.LENGTH_SHORT).show()
    }

    private fun openWebApp(app: WebApp) {
        WebViewLauncher.startWebView(app, this)
    }

    private fun openAppSettings(appId: Int) {
        val intent = Intent(this, WebAppSettingsActivity::class.java)
        intent.putExtra(Const.INTENT_WEBAPPID, appId)
        startActivity(intent)
    }

    private fun openDownloadHistory(appId: Int) {
        val intent = Intent(this, DownloadHistoryActivity::class.java)
        intent.putExtra(WaosConstants.EXTRA_DOWNLOAD_APP_ID, appId)
        startActivity(intent)
    }

    private fun openClipboard(appId: Int) {
        val intent = Intent(this, ClipboardManagerActivity::class.java)
        intent.putExtra(WaosConstants.EXTRA_CLIPBOARD_APP_ID, appId)
        startActivity(intent)
    }

    private fun openCredentials(appId: Int) {
        val intent = Intent(this, CredentialVaultActivity::class.java)
        intent.putExtra(WaosConstants.EXTRA_WAOS_APP_ID, appId)
        startActivity(intent)
    }

    private fun cloneWebApp(app: WebApp) {
        DataManager.getInstance().cloneWebApp(app.ID)
        loadApps()
        Toast.makeText(this, "Web App cloned", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDeleteWebApp(app: WebApp) {
        AlertDialog.Builder(this)
            .setTitle("Delete Web App")
            .setMessage("Are you sure you want to delete ${app.getTitle()}?")
            .setPositiveButton("Delete") { _, _ ->
                DataManager.getInstance().deleteWebApp(app.ID)
                loadApps()
                Toast.makeText(this, "Web App deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun toggleLockWebApp(app: WebApp) {
        app.isBiometricProtection = !app.isBiometricProtection
        DataManager.getInstance().replaceWebApp(app)
        loadApps()
        val message = if (app.isBiometricProtection) "Lock enabled" else "Lock disabled"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

private class WaosAppAdapter(
    apps: List<WebApp>,
    private val onOpenApp: (WebApp) -> Unit,
    private val onShowDownloads: (WebApp) -> Unit,
    private val onShowClipboard: (WebApp) -> Unit,
    private val onShowCredentials: (WebApp) -> Unit,
    private val onShowOptions: (WebApp) -> Unit
) : RecyclerView.Adapter<WaosAppViewHolder>() {

    private var originalApps: List<WebApp> = apps.toList()
    var currentApps: List<WebApp> = apps
        private set
    private var apps: List<WebApp> = apps

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): WaosAppViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.card_waos_app, parent, false)
        return WaosAppViewHolder(view)
    }

    override fun onBindViewHolder(holder: WaosAppViewHolder, position: Int) {
        val app = apps[position]
        holder.title.text = app.getTitle()
        holder.subtitle.text = app.getBaseUrl()
        holder.status.text = app.group.ifBlank { "Workspace" }
        holder.openButton.setOnClickListener { onOpenApp(app) }
        holder.downloadsButton.setOnClickListener { onShowDownloads(app) }
        holder.clipboardButton.setOnClickListener { onShowClipboard(app) }
        holder.credentialsButton.setOnClickListener { onShowCredentials(app) }
        holder.itemView.setOnLongClickListener {
            onShowOptions(app)
            true
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateWebApps(newApps: List<WebApp>) {
        originalApps = newApps
        currentApps = newApps
        filter("")
    }

    fun filter(query: String) {
        apps = if (query.isBlank()) {
            originalApps
        } else {
            originalApps.filter {
                it.getTitle().contains(query, ignoreCase = true) ||
                    it.getBaseUrl().contains(query, ignoreCase = true) ||
                    it.group.contains(query, ignoreCase = true)
            }
        }
        currentApps = apps
        notifyDataSetChanged()
    }
}

private class WaosAppViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
    val title: android.widget.TextView = view.findViewById(R.id.waos_app_title)
    val subtitle: android.widget.TextView = view.findViewById(R.id.waos_app_subtitle)
    val status: android.widget.TextView = view.findViewById(R.id.waos_app_status)
    val openButton: android.widget.Button = view.findViewById(R.id.button_open_app)
    val downloadsButton: android.widget.Button = view.findViewById(R.id.button_download_history)
    val clipboardButton: android.widget.Button = view.findViewById(R.id.button_clipboard)
    val credentialsButton: android.widget.Button = view.findViewById(R.id.button_credentials)
}
