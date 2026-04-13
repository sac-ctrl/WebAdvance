package com.cylonid.nativealpha.waos.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.model.DownloadRecord
import java.text.DateFormat
import java.util.*

class DownloadHistoryAdapter(
    private val downloads: List<DownloadRecord>,
    private val onOpen: (DownloadRecord) -> Unit
) : RecyclerView.Adapter<DownloadHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = downloads[position]
        holder.name.text = record.fileName
        holder.details.text = "${record.mimeType} · ${humanReadableSize(record.sizeBytes)} · ${DateFormat.getDateTimeInstance().format(Date(record.timestamp))}"
        holder.itemView.setOnClickListener { onOpen(record) }
    }

    override fun getItemCount(): Int = downloads.size

    private fun humanReadableSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex])
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.download_name)
        val details: TextView = view.findViewById(R.id.download_details)
    }
}
