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
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.waos.model.ClipboardItem
import com.cylonid.nativealpha.waos.model.ClipboardRepository
import com.cylonid.nativealpha.waos.util.WaosConstants
import com.google.android.material.snackbar.Snackbar

class ClipboardManagerActivity : AppCompatActivity() {
    private lateinit var clipboardRecyclerView: RecyclerView
    private lateinit var adapter: ClipboardAdapter
    private lateinit var clipboardItems: MutableList<ClipboardItem>
    private lateinit var searchInput: EditText
    private lateinit var itemCountText: TextView
    private var appId: Int = -1
    private var maxItems: Int = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clipboard_manager)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Clipboard History"

        appId = intent.getIntExtra(WaosConstants.EXTRA_CLIPBOARD_APP_ID, -1)

        maxItems = if (appId >= 0) {
            try {
                val webapp = DataManager.getInstance().getWebAppIgnoringGlobalOverride(appId, false)
                webapp?.clipboardMaxItems ?: 50
            } catch (_: Exception) { 50 }
        } else 200

        val exportButton = findViewById<Button>(R.id.button_clipboard_export)
        val clearButton = findViewById<Button>(R.id.button_clipboard_clear)
        searchInput = findViewById(R.id.clipboard_search_input)
        itemCountText = findViewById(R.id.clipboard_item_count)
        clipboardRecyclerView = findViewById(R.id.clipboard_recycler_view)
        clipboardRecyclerView.layoutManager = LinearLayoutManager(this)

        loadClipboardItems()

        adapter = ClipboardAdapter(clipboardItems) { item ->
            hapticTap()
            pasteToSystemClipboard(item)
        }
        clipboardRecyclerView.adapter = adapter
        setupSwipeToDelete()
        setupSearch()

        exportButton.setOnClickListener { hapticTap(); shareClipboardItems() }
        clearButton.setOnClickListener { hapticTap(); clearClipboardHistory() }

        updateItemCount()
    }

    private fun loadClipboardItems() {
        clipboardItems = if (appId >= 0) {
            ClipboardRepository.loadClipboardItems(this).filter { it.appId == appId }.toMutableList()
        } else {
            ClipboardRepository.loadClipboardItems(this)
        }
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

    private fun setupSearch() {
        searchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterItems(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterItems(query: String) {
        val filtered = if (query.isBlank()) {
            clipboardItems
        } else {
            clipboardItems.filter { it.text.contains(query, ignoreCase = true) }
        }
        adapter.updateItems(filtered.toMutableList())
        updateItemCount()
    }

    private fun updateItemCount() {
        itemCountText?.text = "${clipboardItems.size} / $maxItems items"
    }

    private fun setupSwipeToDelete() {
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                hapticTap()
                val position = viewHolder.adapterPosition
                val item = clipboardItems[position]
                clipboardItems.removeAt(position)
                ClipboardRepository.saveClipboardItems(this@ClipboardManagerActivity, clipboardItems)
                adapter.notifyItemRemoved(position)
                updateItemCount()
                Snackbar.make(clipboardRecyclerView, "Item removed", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        clipboardItems.add(position, item)
                        ClipboardRepository.saveClipboardItems(this@ClipboardManagerActivity, clipboardItems)
                        adapter.notifyItemInserted(position)
                        updateItemCount()
                    }
                    .show()
            }
        })
        touchHelper.attachToRecyclerView(clipboardRecyclerView)
    }

    private fun shareClipboardItems() {
        if (clipboardItems.isEmpty()) {
            Snackbar.make(clipboardRecyclerView, "No clipboard items to share", Snackbar.LENGTH_SHORT).show()
            return
        }
        val content = clipboardItems.joinToString(separator = "\n\n---\n\n") { item ->
            val label = DateFormat.getDateFormat(this).format(item.timestamp)
            "${item.text}\n[$label]"
        }
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, content)
            .putExtra(Intent.EXTRA_SUBJECT, "Clipboard History")
        startActivity(Intent.createChooser(intent, "Share clipboard items"))
    }

    private fun clearClipboardHistory() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear Clipboard History")
            .setMessage("Are you sure you want to clear all ${clipboardItems.size} items?")
            .setPositiveButton("Clear") { _, _ ->
                clipboardItems.clear()
                if (appId >= 0) {
                    ClipboardRepository.clearAppClipboard(this, appId)
                } else {
                    ClipboardRepository.saveClipboardItems(this, clipboardItems)
                }
                adapter.notifyDataSetChanged()
                updateItemCount()
                Snackbar.make(clipboardRecyclerView, "Clipboard history cleared", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun pasteToSystemClipboard(item: ClipboardItem) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WAOS Clipboard", item.text)
        clipboard.setPrimaryClip(clip)
        Snackbar.make(clipboardRecyclerView, "Copied to clipboard", Snackbar.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
