package com.oamk.kiertotalous.ui

import androidx.databinding.ObservableList
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewAdapterOnListChangedCallback<T>(private val adapter: RecyclerView.Adapter<*>) : ObservableList.OnListChangedCallback<ObservableList<T>>() {
    override fun onChanged(sender: ObservableList<T>) {
        // adapter.notifyDataSetChanged()
    }

    override fun onItemRangeRemoved(sender: ObservableList<T>, positionStart: Int, itemCount: Int) {
        adapter.notifyItemRangeRemoved(positionStart, itemCount)
    }

    override fun onItemRangeMoved(sender: ObservableList<T>, fromPosition: Int, toPosition: Int, itemCount: Int) { }

    override fun onItemRangeInserted(sender: ObservableList<T>, positionStart: Int, itemCount: Int) {
        adapter.notifyItemRangeInserted(positionStart, itemCount)
    }

    override fun onItemRangeChanged(sender: ObservableList<T>, positionStart: Int, itemCount: Int) {
        adapter.notifyItemRangeChanged(positionStart, itemCount)
    }
}

fun <T> ObservableList<T>.subscribe(adapter: RecyclerView.Adapter<*>) {
    addOnListChangedCallback(RecyclerViewAdapterOnListChangedCallback(adapter))
}