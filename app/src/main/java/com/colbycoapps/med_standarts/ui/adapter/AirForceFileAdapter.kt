package com.colbycoapps.med_standarts.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.colbycoapps.med_standarts.R

class AirForceFileAdapter(
    private var items: List<Pair<String, String>>,
    private val listener: OnFileClickListener
) : RecyclerView.Adapter<AirForceFileAdapter.FileViewHolder>() {

    interface OnFileClickListener {
        /**
         * itemName: назва папки або файлу
         * itemValue: "folder" або URL
         */
        fun onFileClick(itemName: String, itemValue: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val (itemName, itemValue) = items[position]
        holder.fileName.text = itemName

        // Клік: якщо "folder" → підпапка, інакше URL
        holder.itemView.setOnClickListener {
            listener.onFileClick(itemName, itemValue)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<Pair<String, String>>) {
        items = newItems
        notifyDataSetChanged()
    }

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.fileName)
    }
}
