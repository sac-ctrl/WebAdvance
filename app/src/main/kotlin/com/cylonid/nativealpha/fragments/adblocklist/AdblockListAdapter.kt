package com.cylonid.nativealpha.fragments.adblocklist

import android.app.Activity

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.AdblockConfig
import com.cylonid.nativealpha.model.DataManager
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter

class AdblockListAdapter(dataSet: List<AdblockConfig> = emptyList(), private val activityOfFragment: Activity)
    : DragDropSwipeAdapter<AdblockConfig, AdblockListAdapter.ViewHolder>(dataSet) {

    class ViewHolder(webAppLayout: View) : DragDropSwipeAdapter.ViewHolder(webAppLayout) {

        val titleView: TextView = itemView.findViewById(R.id.adblock_title)
        val subtitleView: TextView = itemView.findViewById(R.id.adblock_subtitle)
    }

    override fun getViewHolder(itemView: View) = ViewHolder(itemView)
    override fun onBindViewHolder(item: AdblockConfig, viewHolder: ViewHolder, position: Int) {
        viewHolder.titleView.text = item.label
        viewHolder.subtitleView.text = item.value

    }

    fun updateAdblockList() {
        dataSet = DataManager.getInstance().settings.globalWebApp.adBlockSettings
    }

    private fun getString(@StringRes resId: Int): String {
      return activityOfFragment.getString(resId)

    }

    override fun getViewToTouchToStartDraggingItem(item: AdblockConfig, viewHolder: ViewHolder, position: Int) = viewHolder.titleView

}