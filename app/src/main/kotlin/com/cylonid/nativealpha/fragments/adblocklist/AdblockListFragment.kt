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

class AdblockListFragment : Fragment(R.layout.fragment_adblock_list) {
    private lateinit var adapter: AdblockListAdapter
    private lateinit var list: DragDropSwipeRecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val globalWebApp = DataManager.getInstance().settings.globalWebApp
        adapter = AdblockListAdapter(globalWebApp.adBlockSettings, requiredActivity())

        list = view.findViewById(R.id.adblock_list)
        list.layoutManager = LinearLayoutManager(requiredActivity())
        list.adapter = adapter
        list.swipeListener = onItemSwipeListener
        list.orientation =
            DragDropSwipeRecyclerView.ListOrientation.VERTICAL_LIST_WITH_VERTICAL_DRAGGING
        list.disableDragDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.UP)
        list.disableDragDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.DOWN)
        list.disableSwipeDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.RIGHT)

    }


    public fun updateAdblockList() {
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
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.question_delete_item))
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    DataManager.getInstance().apply {
                        val list = settings.globalWebApp.adBlockSettings.toMutableList()
                        list.removeAt(position)
                        settings.globalWebApp.adBlockSettings = list
                        saveGlobalSettings()
                    }
                    updateAdblockList()
                }
                .setNegativeButton(R.string.cancel) { _: DialogInterface, _: Int ->
                    updateAdblockList()
                }
                .create()
                .show()
            return true
        }
    }

}