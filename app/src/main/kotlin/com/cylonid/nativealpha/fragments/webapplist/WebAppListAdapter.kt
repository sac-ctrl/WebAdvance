package com.cylonid.nativealpha.fragments.webapplist

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.transition.Visibility
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.WebAppSettingsActivity
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.util.Const
import com.cylonid.nativealpha.util.WebViewLauncher.startWebView
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import com.google.android.material.internal.VisibilityAwareImageButton
import java.util.ArrayList

class WebAppListAdapter(dataSet: List<WebApp> = emptyList(), private val activityOfFragment: Activity)
    : DragDropSwipeAdapter<WebApp, WebAppListAdapter.ViewHolder>(dataSet) {

    class ViewHolder(webAppLayout: View) : DragDropSwipeAdapter.ViewHolder(webAppLayout) {
        val dragAnchor : ImageView = itemView.findViewById(R.id.dragAnchor)
        val titleView: TextView = itemView.findViewById(R.id.btn_title)

    }

    override fun getViewHolder(itemView: View) = ViewHolder(itemView)
    override fun onBindViewHolder(item: WebApp, viewHolder: ViewHolder, position: Int) {
        viewHolder.titleView.text = item.title
        viewHolder.titleView.setOnClickListener {
            openWebView(
                item
            )
        }
    }

    fun updateWebAppList() {
        dataSet = DataManager.getInstance().activeWebsites
    }


    private fun openWebView(webapp: WebApp) {
        startWebView(webapp, activityOfFragment)
    }


    override fun getViewToTouchToStartDraggingItem(item: WebApp, viewHolder: ViewHolder, position: Int) = viewHolder.dragAnchor

}