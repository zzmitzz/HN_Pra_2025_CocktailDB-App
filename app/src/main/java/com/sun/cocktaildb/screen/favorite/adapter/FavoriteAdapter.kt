package com.sun.cocktaildb.screen.favorite.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.databinding.ItemFavoriteCocktailBinding
import com.sun.cocktaildb.utils.ImageLoader
import com.sun.cocktaildb.R

class FavoriteAdapter(
    private val onCocktailClickListener: (Cocktail) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {

    private var items: List<Cocktail> = emptyList()

    class ViewHolder(val binding: ItemFavoriteCocktailBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFavoriteCocktailBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.name
        
        // Load cocktail image
        ImageLoader.loadImage(holder.binding.ivThumb, item.imageUrl, R.drawable.placeholder)
        
        holder.binding.tvDesc.text = buildString {
            item.ingredients.take(3).forEachIndexed { index, s ->
                append(s)
                if (index < 2) append("\n")
            }
        }
        holder.binding.root.setOnClickListener { onCocktailClickListener(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateCocktails(newItems: List<Cocktail>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun submit(newItems: List<Cocktail>) {
        items = newItems
        notifyDataSetChanged()
    }
}
