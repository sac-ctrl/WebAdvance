package com.cylonid.nativealpha.waos.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.model.ClipboardItem
import com.cylonid.nativealpha.waos.model.ClipboardRepository
import com.cylonid.nativealpha.waos.util.WaosConstants
import com.google.android.material.snackbar.Snackbar

class ClipboardManagerActivity : AppCompatActivity() {
    private lateinit var clipboardRecyclerView: RecyclerView
    private lateinit var adapter: ClipboardAdapter
    private lateinit var clipboardItems: MutableList<ClipboardItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clipboard_manager)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val exportButton = findViewById<Button>(R.id.button_clipboard_export)
        val clearButton = findViewById<Button>(R.id.button_clipboard_clear)
        clipboardRecyclerView = findViewById(R.id.clipboard_recycler_view)
        clipboardRecyclerView.layoutManager = LinearLayoutManager(this)

        val appId = intent.getIntExtra(WaosConstants.EXTRA_CLIPBOARD_APP_ID, -1)
        clipboardItems = if (appId >= 0) {
            ClipboardRepository.loadClipboardItems(this).filter { it.appId == appId }.toMutableList()
        } else {
            ClipboardRepository.loadClipboardItems(this)
        }

        adapter = ClipboardAdapter(clipboardItems) { item -> pasteToSystemClipboard(item) }
        clipboardRecyclerView.adapter = adapter
        setupSwipeToDelete()

        exportButton.setOnClickListener { shareClipboardItems() }
        clearButton.setOnClickListener { clearClipboardHistory() }
    }

    private fun setupSwipeToDelete() {
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = clipboardItems[position]
                clipboardItems.removeAt(position)
                ClipboardRepository.saveClipboardItems(this@ClipboardManagerActivity, clipboardItems)
                adapter.notifyItemRemoved(position)
                Snackbar.make(clipboardRecyclerView, "Clipboard item removed", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        clipboardItems.add(position, item)
                        ClipboardRepository.saveClipboardItems(this@ClipboardManagerActivity, clipboardItems)
                        adapter.notifyItemInserted(position)
                    }
                    .show()
            }
        })
        touchHelper.attachToRecyclerView(clipboardRecyclerView)
    }

    private fun shareClipboardItems() {
        if (clipboardItems.isEmpty()) {
            return
        }
        val content = clipboardItems.joinToString(separator = "\n\n") { item ->
            val label = DateFormat.getDateFormat(this).format(item.timestamp)
            "${item.text}\n$label"
        }
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, content)
        startActivity(Intent.createChooser(intent, "Share clipboard items"))
    }

    private fun clearClipboardHistory() {
        clipboardItems.clear()
        ClipboardRepository.saveClipboardItems(this, clipboardItems)
        adapter.notifyDataSetChanged()
    }

    private fun pasteToSystemClipboard(item: ClipboardItem) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WAOS Clipboard", item.text)
        clipboard.setPrimaryClip(clip)
        Snackbar.make(clipboardRecyclerView, "Copied to system clipboard", Snackbar.LENGTH_SHORT).show()
    }
}
