package com.cylonid.nativealpha.waos.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
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
    private lateinit var adapter: WaosGroupedAdapter
    private var isGridMode = false
    private var currentSortMode = SortMode.ORDER
    private var isFolderGroupingEnabled = false
    private var vibrator: Vibrator? = null

    private enum class SortMode { ORDER, NAME, GROUP }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waos_dashboard)

        DataManager.getInstance().loadAppData()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

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
        dashboardRecyclerView.itemAnimator = DefaultItemAnimator()

        adapter = WaosGroupedAdapter(
            items = emptyList(),
            onOpenApp = { app -> hapticTap(); openWebApp(app) },
            onShowDownloads = { app -> hapticTap(); openDownloadHistory(app.ID) },
            onShowClipboard = { app -> hapticTap(); openClipboard(app.ID) },
            onShowCredentials = { app -> hapticTap(); openCredentials(app.ID) },
            onShowOptions = { app -> hapticLongPress(); showAppActions(app) }
        )
        dashboardRecyclerView.adapter = adapter

        setupLayoutManager()
        setupSearch()
        setupRefresh()
        setupLayoutToggle()
        setupSort()
        setupAddApp()
        setupDragAndDrop()
        setupFolderGroupingToggle()
        loadApps()

        val launchBubbleButton = findViewById<Button>(R.id.button_launch_floating)
        launchBubbleButton?.setOnClickListener {
            hapticTap()
            startService(Intent(this, com.cylonid.nativealpha.waos.service.FloatingWindowService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun setupLayoutManager() {
        dashboardRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun hapticTap() {
        try {
            vibrator?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(30)
                }
            }
        } catch (_: Exception) {}
    }

    private fun hapticLongPress() {
        try {
            vibrator?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(60)
                }
            }
        } catch (_: Exception) {}
    }

    private fun loadApps(query: String = searchInput.text?.toString() ?: "") {
        val allApps = when (currentSortMode) {
            SortMode.NAME -> DataManager.getInstance().getActiveWebsites().sortedBy { it.title.lowercase() }
            SortMode.GROUP -> DataManager.getInstance().getActiveWebsites().sortedWith(compareBy({ it.group.lowercase() }, { it.order }))
            SortMode.ORDER -> DataManager.getInstance().getActiveWebsites().sortedBy { it.order }
        }

        val filtered = if (query.isBlank()) allApps else allApps.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.baseUrl.contains(query, ignoreCase = true) ||
                it.group.contains(query, ignoreCase = true)
        }

        val items: List<WaosListItem> = if (isFolderGroupingEnabled) {
            buildGroupedItems(filtered)
        } else {
            filtered.map { WaosListItem.AppItem(it) }
        }

        adapter.updateItems(items)
        val total = filtered.size
        val suffix = if (total == 1) "app" else "apps"
        appCountText.text = "$total $suffix"
        swipeRefreshLayout.isRefreshing = false
    }

    private fun buildGroupedItems(apps: List<WebApp>): List<WaosListItem> {
        val grouped = apps.groupBy { it.group.ifBlank { "Default" } }
        val result = mutableListOf<WaosListItem>()
        val sortedGroups = grouped.keys.sorted()
        for (groupName in sortedGroups) {
            val groupApps = grouped[groupName] ?: continue
            result.add(WaosListItem.GroupHeader(groupName, groupApps.size))
            result.addAll(groupApps.map { WaosListItem.AppItem(it) })
        }
        return result
    }

    private fun setupFolderGroupingToggle() {
        val groupToggleButton = findViewById<Button>(R.id.button_group_toggle)
        groupToggleButton?.setOnClickListener {
            hapticTap()
            isFolderGroupingEnabled = !isFolderGroupingEnabled
            groupToggleButton.text = if (isFolderGroupingEnabled) "Ungrouped" else "Grouped"
            loadApps()
        }
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadApps(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSort() {
        sortAppsButton.setOnClickListener {
            hapticTap()
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
            hapticTap()
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
        refreshButton?.setOnClickListener { hapticTap(); loadApps() }
        swipeRefreshLayout.setOnRefreshListener { loadApps() }
    }

    private fun setupLayoutToggle() {
        toggleLayoutButton.text = if (isGridMode) "List" else "Grid"
        toggleLayoutButton.setOnClickListener {
            hapticTap()
            isGridMode = !isGridMode
            toggleLayoutButton.text = if (isGridMode) "List" else "Grid"
            dashboardRecyclerView.layoutManager = if (isGridMode) GridLayoutManager(this, 2) else LinearLayoutManager(this)
        }
    }

    private fun setupDragAndDrop() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromType = viewHolder.itemViewType
                val toType = target.itemViewType
                if (fromType != WaosGroupedAdapter.TYPE_APP || toType != WaosGroupedAdapter.TYPE_APP) return false
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                val currentItems = adapter.currentItems.toMutableList()
                Collections.swap(currentItems, from, to)
                adapter.updateItems(currentItems)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder.itemViewType != WaosGroupedAdapter.TYPE_APP) return 0
                return super.getMovementFlags(recyclerView, viewHolder)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                saveAppOrder()
            }
        })
        itemTouchHelper.attachToRecyclerView(dashboardRecyclerView)
    }

    private fun saveAppOrder() {
        val appItems = adapter.currentItems.filterIsInstance<WaosListItem.AppItem>()
        appItems.forEachIndexed { index, item ->
            item.app.order = index + 1
            DataManager.getInstance().replaceWebApp(item.app)
        }
    }

    private fun showAppActions(app: WebApp) {
        val lockLabel = if (app.isBiometricProtection) "Unlock" else "Lock"
        val actions = arrayOf("Open", "Settings", "Download History", "Clipboard", "Vault", "Clone", "Delete", lockLabel, "Copy URL", "Launch Floating Bubble")
        AlertDialog.Builder(this)
            .setTitle(app.title)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> { hapticTap(); openWebApp(app) }
                    1 -> { hapticTap(); openAppSettings(app.ID) }
                    2 -> { hapticTap(); openDownloadHistory(app.ID) }
                    3 -> { hapticTap(); openClipboard(app.ID) }
                    4 -> { hapticTap(); openCredentials(app.ID) }
                    5 -> { hapticTap(); cloneWebApp(app) }
                    6 -> { hapticTap(); confirmDeleteWebApp(app) }
                    7 -> { hapticTap(); toggleLockWebApp(app) }
                    8 -> { hapticTap(); copyAppUrl(app) }
                    9 -> { hapticTap(); startService(Intent(this, com.cylonid.nativealpha.waos.service.FloatingWindowService::class.java)) }
                }
            }
            .show()
    }

    private fun copyAppUrl(app: WebApp) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("WAOS App URL", app.baseUrl))
        Toast.makeText(this, "App URL copied", Toast.LENGTH_SHORT).show()
    }

    private fun openWebApp(app: WebApp) { WebViewLauncher.startWebView(app, this) }

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
            .setMessage("Are you sure you want to delete ${app.title}?")
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

sealed class WaosListItem {
    data class GroupHeader(val name: String, val count: Int) : WaosListItem()
    data class AppItem(val app: WebApp) : WaosListItem()
}

class WaosGroupedAdapter(
    items: List<WaosListItem>,
    private val onOpenApp: (WebApp) -> Unit,
    private val onShowDownloads: (WebApp) -> Unit,
    private val onShowClipboard: (WebApp) -> Unit,
    private val onShowCredentials: (WebApp) -> Unit,
    private val onShowOptions: (WebApp) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_APP = 1
    }

    var currentItems: List<WaosListItem> = items
        private set

    override fun getItemViewType(position: Int): Int = when (currentItems[position]) {
        is WaosListItem.GroupHeader -> TYPE_HEADER
        is WaosListItem.AppItem -> TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_group_header, parent, false)
            GroupHeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.card_waos_app, parent, false)
            WaosAppViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = currentItems[position]) {
            is WaosListItem.GroupHeader -> (holder as GroupHeaderViewHolder).bind(item)
            is WaosListItem.AppItem -> (holder as WaosAppViewHolder).bind(item.app)
        }
    }

    override fun getItemCount(): Int = currentItems.size

    fun updateItems(newItems: List<WaosListItem>) {
        currentItems = newItems
        notifyDataSetChanged()
    }

    private inner class GroupHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.group_header_name)
        val countText: TextView = view.findViewById(R.id.group_header_count)

        fun bind(header: WaosListItem.GroupHeader) {
            nameText.text = header.name
            val suffix = if (header.count == 1) "app" else "apps"
            countText.text = "${header.count} $suffix"
        }
    }

    private inner class WaosAppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.waos_app_title)
        val subtitle: TextView = view.findViewById(R.id.waos_app_subtitle)
        val groupText: TextView = view.findViewById(R.id.waos_app_group)
        val status: TextView = view.findViewById(R.id.waos_app_status)
        val lastUpdated: TextView = view.findViewById(R.id.waos_app_last_updated)
        val notificationBadge: TextView = view.findViewById(R.id.waos_notification_badge)
        val openButton: Button = view.findViewById(R.id.button_open_app)
        val downloadsButton: Button = view.findViewById(R.id.button_download_history)
        val clipboardButton: Button = view.findViewById(R.id.button_clipboard)
        val credentialsButton: Button = view.findViewById(R.id.button_credentials)

        fun bind(app: WebApp) {
            title.text = app.title
            subtitle.text = app.baseUrl
            groupText.text = app.group.ifBlank { "Default" }
            status.text = app.group.ifBlank { "Active" }

            val lastUsed = try {
                val field = app.javaClass.getDeclaredField("lastUsed")
                field.isAccessible = true
                field.get(app) as? Date
            } catch (_: Exception) { null }

            if (lastUsed != null) {
                lastUpdated.visibility = View.VISIBLE
                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                lastUpdated.text = "Last used: ${sdf.format(lastUsed)}"
            } else {
                lastUpdated.visibility = View.GONE
            }

            val notifCount = try {
                val field = app.javaClass.getDeclaredField("notificationCount")
                field.isAccessible = true
                field.getInt(app)
            } catch (_: Exception) { 0 }

            if (notifCount > 0) {
                notificationBadge.visibility = View.VISIBLE
                notificationBadge.text = if (notifCount > 99) "99+" else notifCount.toString()
            } else {
                notificationBadge.visibility = View.GONE
            }

            openButton.setOnClickListener {
                animatePress(itemView)
                onOpenApp(app)
            }
            downloadsButton.setOnClickListener { onShowDownloads(app) }
            clipboardButton.setOnClickListener { onShowClipboard(app) }
            credentialsButton.setOnClickListener { onShowCredentials(app) }
            itemView.setOnLongClickListener {
                onShowOptions(app)
                true
            }
        }

        private fun animatePress(view: View) {
            val scaleDown = android.animation.AnimatorSet().apply {
                playTogether(
                    android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.96f),
                    android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.96f)
                )
                duration = 80
            }
            val scaleUp = android.animation.AnimatorSet().apply {
                playTogether(
                    android.animation.ObjectAnimator.ofFloat(view, "scaleX", 0.96f, 1f),
                    android.animation.ObjectAnimator.ofFloat(view, "scaleY", 0.96f, 1f)
                )
                duration = 80
            }
            android.animation.AnimatorSet().apply {
                playSequentially(scaleDown, scaleUp)
                start()
            }
        }
    }
}
