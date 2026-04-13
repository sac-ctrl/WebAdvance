package com.cylonid.nativealpha.waos.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.model.DownloadRepository
import com.cylonid.nativealpha.waos.util.WaosConstants

class DownloadHistoryActivity : AppCompatActivity() {
    private lateinit var downloadHistoryRecyclerView: RecyclerView
    private lateinit var adapter: DownloadHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_history)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        downloadHistoryRecyclerView = findViewById(R.id.download_history_recycler_view)
        downloadHistoryRecyclerView.layoutManager = LinearLayoutManager(this)

        val appId = intent.getIntExtra(WaosConstants.EXTRA_DOWNLOAD_APP_ID, -1)
        val downloads = if (appId >= 0) {
            DownloadRepository.loadDownloads(this).filter { it.appId == appId }
        } else {
            DownloadRepository.loadDownloads(this)
        }

        adapter = DownloadHistoryAdapter(downloads) { record -> openFile(record) }
        downloadHistoryRecyclerView.adapter = adapter
    }

    private fun openFile(record: com.cylonid.nativealpha.waos.model.DownloadRecord) {
        val intent = Intent(this, UniversalFileViewerActivity::class.java)
        intent.putExtra(WaosConstants.EXTRA_FILE_PATH, record.uriPath)
        startActivity(intent)
    }
}
