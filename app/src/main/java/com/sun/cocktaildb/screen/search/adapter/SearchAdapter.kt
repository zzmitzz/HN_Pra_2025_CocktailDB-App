package com.sun.cocktaildb.screen.search.adapter

import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sun.cocktaildb.R
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.databinding.ItemSearchCocktailBinding
import com.sun.cocktaildb.utils.ImageLoader
import com.sun.cocktaildb.utils.Constants

class SearchAdapter(
    private val onCocktailClicked: (Cocktail) -> Unit,
    private val onFavoriteClickListener: (Cocktail, Boolean) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    private var cocktails: MutableList<Cocktail> = mutableListOf()
    private var currentSearchQuery: String = ""

    fun updateCocktailFavoriteStatus(cocktailId: String, isFavorite: Boolean) {
        val index = cocktails.indexOfFirst { it.id == cocktailId }
        if (index != -1) {
            val updatedCocktail = cocktails[index].copy(isFavorite = isFavorite)
            cocktails[index] = updatedCocktail
            notifyItemChanged(index)
        }
    }
    
    fun getCurrentCocktails(): List<Cocktail> {
        return cocktails.toList()
    }

    fun updateCocktails(newCocktails: List<Cocktail>, searchQuery: String = "") {
        cocktails.clear()
        cocktails.addAll(newCocktails)
        currentSearchQuery = searchQuery
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemSearchCocktailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(cocktails[position])
    }

    override fun getItemCount(): Int = cocktails.size

    inner class SearchViewHolder(
        private val binding: ItemSearchCocktailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCocktailClicked(cocktails[position])
                }
            }

            binding.ivFavorite.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val cocktail = cocktails[position]
                    val newFavoriteStatus = !cocktail.isFavorite
                    onFavoriteClickListener(cocktail, newFavoriteStatus)
                }
            }
        }

        fun bind(cocktail: Cocktail) {
            // Highlight matching characters in cocktail name
            binding.tvCocktailName.text = highlightMatchingText(cocktail.name, currentSearchQuery)
            
            // Show category instead of description
            binding.tvCocktailDescription.text = cocktail.category
            
            // Load cocktail image
            if (!cocktail.imageUrl.isNullOrEmpty()) {
                ImageLoader.loadImage(
                    binding.ivCocktailImage,
                    cocktail.imageUrl,
                    R.drawable.placeholder
                )
            } else {
                binding.ivCocktailImage.setImageResource(R.drawable.placeholder)
            }

            // Set favorite button state
            binding.ivFavorite.isSelected = cocktail.isFavorite
            binding.ivFavorite.setImageResource(
                if (cocktail.isFavorite) R.drawable.ic_favorite_filled_black_24dp
                else R.drawable.ic_favorite_border_black_24dp
            )
        }
        
        private fun highlightMatchingText(text: String, query: String): SpannableString {
            val spannableString = SpannableString(text)
            if (query.isNotEmpty()) {
                val textLower = text.lowercase()
                val queryLower = query.lowercase()
                var startIndex = 0
                
                // Find all matches and sort by position (left to right priority)
                val matches = mutableListOf<Pair<Int, Int>>()
                while (true) {
                    val index = textLower.indexOf(queryLower, startIndex)
                    if (index == -1) break
                    matches.add(Pair(index, index + query.length))
                    startIndex = index + 1
                }
                
                matches.forEach { (start, end) ->
                    spannableString.setSpan(
                        BackgroundColorSpan(ContextCompat.getColor(binding.root.context, R.color.yellow_highlight)),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            return spannableString
        }
    }
}
