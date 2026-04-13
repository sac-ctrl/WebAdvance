package com.cylonid.nativealpha.waos.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.model.CredentialItem

class CredentialAdapter(
    private val items: List<CredentialItem>,
    private val onCopy: (CredentialItem) -> Unit
) : RecyclerView.Adapter<CredentialAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_credential_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.username.text = item.username
        holder.itemView.setOnClickListener { onCopy(item) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.credential_title)
        val username: TextView = view.findViewById(R.id.credential_username)
    }
}
