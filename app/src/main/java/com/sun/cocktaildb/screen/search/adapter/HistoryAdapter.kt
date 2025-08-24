package com.sun.cocktaildb.screen.search.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sun.cocktaildb.databinding.ItemSearchHistoryBinding

class HistoryAdapter(
    private val onHistoryItemClickListener: (String) -> Unit,
    private val onHistoryItemDeleteClickListener: (String) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var historyItems = listOf<String>()

    fun updateHistory(newHistory: List<String>) {
        historyItems = newHistory
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemSearchHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(historyItems[position])
    }

    override fun getItemCount(): Int = historyItems.size

    inner class HistoryViewHolder(
        private val binding: ItemSearchHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(query: String) {
            binding.tvHistoryText.text = query
            
            // Click to reuse search query
            binding.root.setOnClickListener {
                onHistoryItemClickListener(query)
            }
            
            // Click to remove from history
            binding.btnRemoveHistory.setOnClickListener {
                onHistoryItemDeleteClickListener(query)
            }
        }
    }
}
