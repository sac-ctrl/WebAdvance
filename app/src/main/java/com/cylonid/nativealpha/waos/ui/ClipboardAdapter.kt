package com.cylonid.nativealpha.waos.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.model.ClipboardItem
import java.text.DateFormat
import java.util.*

class ClipboardAdapter(
    private var clipboardItems: MutableList<ClipboardItem>,
    private val onCopy: (ClipboardItem) -> Unit
) : RecyclerView.Adapter<ClipboardAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clipboard_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = clipboardItems[position]
        val preview = if (item.text.length > 200) item.text.take(200) + "…" else item.text
        holder.clipText.text = preview
        holder.clipMeta.text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.timestamp))
        holder.itemView.setOnClickListener { onCopy(item) }
        holder.itemView.contentDescription = "Clipboard item: ${preview.take(50)}"
    }

    override fun getItemCount(): Int = clipboardItems.size

    fun updateItems(newItems: MutableList<ClipboardItem>) {
        clipboardItems = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clipText: TextView = view.findViewById(R.id.clip_text)
        val clipMeta: TextView = view.findViewById(R.id.clip_meta)
    }
}
