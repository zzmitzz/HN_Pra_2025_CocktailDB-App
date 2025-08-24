package com.sun.cocktaildb.screen.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.sun.cocktaildb.R
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.utils.ImageLoader
import com.sun.cocktaildb.utils.Constants

class PopularCocktailAdapter(
    private val onCocktailClickListener: (Cocktail) -> Unit,
    private val onFavoriteClickListener: (Cocktail, Boolean) -> Unit,
) : RecyclerView.Adapter<PopularCocktailAdapter.CocktailViewHolder>() {
    private val cocktails = mutableListOf<Cocktail>()

    fun updateCocktails(newCocktails: List<Cocktail>) {
        cocktails.clear()
        cocktails.addAll(newCocktails)
        notifyDataSetChanged()
    }
    
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
    
    fun refreshFavorites() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CocktailViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_popular_cocktail, parent, false)
        return CocktailViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CocktailViewHolder,
        position: Int,
    ) {
        holder.bind(cocktails[position])
    }

    override fun getItemCount(): Int = cocktails.size

    inner class CocktailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCocktail: ImageView = itemView.findViewById(R.id.iv_cocktail)
        private val ivFavorite: ImageView = itemView.findViewById(R.id.iv_favorite)
        private val tvCocktailName: TextView = itemView.findViewById(R.id.tv_cocktail_name)
        private val tvCocktailDescription: TextView = itemView.findViewById(R.id.tv_cocktail_description)

        fun bind(cocktail: Cocktail) {
            tvCocktailName.text = cocktail.name
            tvCocktailDescription.text = cocktail.description
            
            // Set favorite icon based on cocktail's favorite status
            updateFavoriteIcon(cocktail.isFavorite)

            // Load image using Android native ImageLoader utility
            val imageUrl = cocktail.imageUrl
                            if (imageUrl.isNotEmpty() && imageUrl != Constants.PLACEHOLDER_IMAGE_URL) {
                ImageLoader.loadImage(ivCocktail, imageUrl, R.drawable.placeholder)
            } else {
                ivCocktail.setImageResource(R.drawable.placeholder)
            }

            // Set click listeners
            itemView.setOnClickListener {
                onCocktailClickListener(cocktail)
            }
            
            ivFavorite.setOnClickListener {
                // Play animation
                val animation = AnimationUtils.loadAnimation(itemView.context, R.anim.favorite_scale)
                ivFavorite.startAnimation(animation)
                
                val newFavoriteStatus = !cocktail.isFavorite
                onFavoriteClickListener(cocktail, newFavoriteStatus)
            }
        }

        private fun updateFavoriteIcon(isFavorite: Boolean) {
            if (isFavorite) {
                ivFavorite.setImageResource(R.drawable.ic_favorite_filled_black_24dp)
            } else {
                ivFavorite.setImageResource(R.drawable.ic_favorite_border_black_24dp)
            }
        }
    }
}
