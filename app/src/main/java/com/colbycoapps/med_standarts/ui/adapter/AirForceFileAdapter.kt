package com.colbycoapps.med_standarts.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.colbycoapps.med_standarts.R

class AirForceFileAdapter(
    private var items: List<Pair<String, String>>,
    private val listener: OnFileClickListener
) : RecyclerView.Adapter<AirForceFileAdapter.FileViewHolder>() {

    interface OnFileClickListener {
        fun onFileClick(itemName: String, itemValue: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val (itemName, itemValue) = items[position]

        holder.fileDescr.visibility = View.GONE // Очистка перед новими даними

        if (itemValue == "folder") {
            when(itemName)
            {
                "AFIs" ->  holder.fileName.text = "Other AFIs"
                "bomc" ->  holder.fileName.text = "BOMC"
                "fsToolkit" ->  holder.fileName.text = "Flight Surgeon Toolkit"
                "main" ->  holder.fileName.text = "Main Documents"
                else -> holder.fileName.text = itemName
            }


            holder.fileDescr.text = ""
        } else {
            val strings = itemName.split("#")
            holder.fileName.text = strings.getOrNull(0) ?: itemName
            holder.fileDescr.text = strings.getOrNull(1) ?: ""
            holder.fileDescr.visibility = if (strings.size > 1) View.VISIBLE else View.GONE
        }

        holder.itemView.setOnClickListener {
            listener.onFileClick(itemName, itemValue)
        }
    }


    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<Pair<String, String>>) {
        val diffCallback = FileDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.fileName)
        val fileDescr: TextView = view.findViewById(R.id.fileDescr)
        val view: View = view.findViewById(R.id.viewFile)
    }
}
