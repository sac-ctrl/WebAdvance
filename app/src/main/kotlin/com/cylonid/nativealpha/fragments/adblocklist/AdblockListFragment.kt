package com.cylonid.nativealpha.fragments.adblocklist

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.AdblockConfig
import com.cylonid.nativealpha.model.DataManager
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.google.android.material.snackbar.Snackbar

class AdblockListFragment : Fragment(R.layout.fragment_adblock_list) {
    private lateinit var adapter: AdblockListAdapter
    private lateinit var list: DragDropSwipeRecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val globalWebApp = DataManager.getInstance().settings.globalWebApp
        adapter = AdblockListAdapter(globalWebApp.adBlockSettings)

        list = view.findViewById(R.id.adblock_list)
        list.layoutManager = LinearLayoutManager(requiredActivity())
        list.adapter = adapter
        list.swipeListener = onItemSwipeListener
        list.orientation =
            DragDropSwipeRecyclerView.ListOrientation.VERTICAL_LIST_WITH_VERTICAL_DRAGGING
        list.disableSwipeDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.RIGHT)

    }


    fun updateAdblockList() {
        adapter.updateAdblockList()
    }

    private fun requiredActivity(): FragmentActivity {
        return requireNotNull(activity) { "AdblockListFragment is not attached to an activity." }
    }

    private val onItemSwipeListener = object : OnItemSwipeListener<AdblockConfig> {

        override fun onItemSwiped(
            position: Int,
            direction: OnItemSwipeListener.SwipeDirection,
            item: AdblockConfig
        ): Boolean {

            DataManager.getInstance().apply {
                val list = settings.globalWebApp.adBlockSettings.toMutableList()
                list.removeAt(position)
                settings.globalWebApp.adBlockSettings = list
                saveGlobalSettings()
            }

            val itemSwipedSnackBar =
                view?.let { Snackbar.make(it, getString(R.string.x_was_removed, item.label), Snackbar.LENGTH_SHORT) }
            itemSwipedSnackBar?.setAction(getString(R.string.undo).uppercase()) {
                DataManager.getInstance().apply {
                    val list = settings.globalWebApp.adBlockSettings.toMutableList()
                    list.add(position, item)
                    settings.globalWebApp.adBlockSettings = list
                    saveGlobalSettings()
                }
                updateAdblockList()
            }
            itemSwipedSnackBar?.show()

            return false
        }
    }

}