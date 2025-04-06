package com.colbycoapps.med_standards.ui.adapter

import androidx.recyclerview.widget.DiffUtil

class FileDiffCallback(
    private val oldList: List<Pair<String, String>>,
    private val newList: List<Pair<String, String>>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].first == newList[newItemPosition].first
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
