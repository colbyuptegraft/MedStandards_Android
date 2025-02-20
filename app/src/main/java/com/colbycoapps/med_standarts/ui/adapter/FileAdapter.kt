package com.colbycoapps.med_standarts.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.colbycoapps.med_standarts.R

class FileAdapter(
    private val files: List<Pair<String, String>>,
    private val listener: OnFileClickListener
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    interface OnFileClickListener {
        fun onFileClick(fileUrl: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val (fileName, fileUrl) = files[position]
        holder.fileName.text = fileName
        holder.itemView.setOnClickListener { listener.onFileClick(fileUrl) }
    }

    override fun getItemCount(): Int = files.size

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.fileName)
    }
}
