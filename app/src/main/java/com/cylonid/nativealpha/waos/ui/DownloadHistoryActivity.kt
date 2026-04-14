package com.cylonid.nativealpha.waos.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.model.DownloadRecord
import com.cylonid.nativealpha.waos.model.DownloadRepository
import com.cylonid.nativealpha.waos.util.WaosConstants
import com.google.android.material.snackbar.Snackbar

class DownloadHistoryActivity : AppCompatActivity() {
    private lateinit var downloadHistoryRecyclerView: RecyclerView
    private lateinit var adapter: DownloadHistoryAdapter
    private lateinit var searchInput: EditText
    private lateinit var filterButton: Button
    private var allDownloads: MutableList<DownloadRecord> = mutableListOf()
    private var currentFilterType = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_history)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        searchInput = findViewById(R.id.search_downloads)
        filterButton = findViewById(R.id.button_filter_type)
        downloadHistoryRecyclerView = findViewById(R.id.download_history_recycler_view)
        downloadHistoryRecyclerView.layoutManager = LinearLayoutManager(this)

        val appId = intent.getIntExtra(WaosConstants.EXTRA_DOWNLOAD_APP_ID, -1)
        allDownloads = if (appId >= 0) {
            DownloadRepository.loadDownloads(this).filter { it.appId == appId }.toMutableList()
        } else {
            DownloadRepository.loadDownloads(this)
        }

        adapter = DownloadHistoryAdapter(allDownloads) { record -> openFile(record) }
        downloadHistoryRecyclerView.adapter = adapter
        setupSearch()
        setupFilter()
        setupSwipeToDelete()
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilter() {
        filterButton.setOnClickListener {
            currentFilterType = when (currentFilterType) {
                "All" -> "Images"
                "Images" -> "Video"
                "Video" -> "Documents"
                "Documents" -> "All"
                else -> "All"
            }
            filterButton.text = currentFilterType
            refreshList()
        }
    }

    private fun refreshList() {
        val query = searchInput.text.toString().trim().lowercase()
        val filtered = allDownloads.filter { record ->
            val matchQuery = query.isBlank() || record.fileName.lowercase().contains(query) || record.mimeType.lowercase().contains(query)
            val matchType = when (currentFilterType) {
                "Images" -> record.mimeType.startsWith("image/")
                "Video" -> record.mimeType.startsWith("video/")
                "Documents" -> record.mimeType.contains("pdf") || record.mimeType.contains("text") || record.mimeType.contains("json") || record.mimeType.contains("xml")
                else -> true
            }
            matchQuery && matchType
        }.sortedWith(compareByDescending<DownloadRecord> { it.timestamp }.thenBy { it.fileName })
        adapter = DownloadHistoryAdapter(filtered) { record -> openFile(record) }
        downloadHistoryRecyclerView.adapter = adapter
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val record = adapter.downloads[pos]
                DownloadRepository.deleteDownload(this@DownloadHistoryActivity, record)
                allDownloads.removeAll { it.timestamp == record.timestamp && it.uriPath == record.uriPath }
                refreshList()
                Snackbar.make(downloadHistoryRecyclerView, "Download removed", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        DownloadRepository.saveDownload(this@DownloadHistoryActivity, record)
                        allDownloads.add(0, record)
                        refreshList()
                    }.show()
            }
        })
        itemTouchHelper.attachToRecyclerView(downloadHistoryRecyclerView)
    }

    private fun openFile(record: DownloadRecord) {
        val intent = Intent(this, UniversalFileViewerActivity::class.java)
        intent.putExtra(WaosConstants.EXTRA_FILE_PATH, record.uriPath)
        startActivity(intent)
    }
}
