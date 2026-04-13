package com.cylonid.nativealpha.waos.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.model.ClipboardItem
import com.cylonid.nativealpha.waos.model.ClipboardRepository
import com.cylonid.nativealpha.waos.util.WaosConstants

class ClipboardManagerActivity : AppCompatActivity() {
    private lateinit var clipboardRecyclerView: RecyclerView
    private lateinit var adapter: ClipboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clipboard_manager)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        clipboardRecyclerView = findViewById(R.id.clipboard_recycler_view)
        clipboardRecyclerView.layoutManager = LinearLayoutManager(this)

        val appId = intent.getIntExtra(WaosConstants.EXTRA_CLIPBOARD_APP_ID, -1)
        val items = if (appId >= 0) {
            ClipboardRepository.loadClipboardItems(this).filter { it.appId == appId }
        } else {
            ClipboardRepository.loadClipboardItems(this)
        }

        adapter = ClipboardAdapter(items) { item -> pasteToSystemClipboard(item) }
        clipboardRecyclerView.adapter = adapter
    }

    private fun pasteToSystemClipboard(item: ClipboardItem) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WAOS Clipboard", item.text)
        clipboard.setPrimaryClip(clip)
    }
}
