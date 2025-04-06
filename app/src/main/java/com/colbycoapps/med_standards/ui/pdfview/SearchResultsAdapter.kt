package com.colbycoapps.med_standards.ui.pdfview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.colbycoapps.med_standards.R

class SearchResultsAdapter(
    private val items: List<SearchResult>,
    private val onItemClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val snippetTextView: TextView = itemView.findViewById(R.id.textSnippet)
        val pageTextView: TextView = itemView.findViewById(R.id.textPageNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = items[position]
        holder.snippetTextView.text = result.snippet
        holder.pageTextView.text = "Page: ${result.pageIndex + 1}"
        holder.itemView.setOnClickListener {
            onItemClick(result)
        }
    }
}
