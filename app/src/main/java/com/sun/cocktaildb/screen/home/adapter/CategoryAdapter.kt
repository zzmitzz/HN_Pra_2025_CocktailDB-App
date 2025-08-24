package com.sun.cocktaildb.screen.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sun.cocktaildb.R
import com.sun.cocktaildb.data.model.Category
import com.sun.cocktaildb.utils.ImageLoader
import com.sun.cocktaildb.utils.Constants

class CategoryAdapter(
    private val onCategoryClickListener: (Category) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
    private val categories = mutableListOf<Category>()

    fun updateCategories(newCategories: List<Category>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CategoryViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int,
    ) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCategory: ImageView = itemView.findViewById(R.id.iv_category)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)

        fun bind(category: Category) {
            tvCategoryName.text = category.name
            
            // Load category image using Android native ImageLoader utility
            val imageUrl = category.imageUrl
                            if (imageUrl.isNotEmpty() && imageUrl != "${Constants.CATEGORY_IMAGE_BASE_URL}/${category.name}.jpg") {
                ImageLoader.loadImage(ivCategory, imageUrl, R.drawable.cocktail_logo)
            } else {
                ivCategory.setImageResource(R.drawable.cocktail_logo)
            }

            itemView.setOnClickListener {
                onCategoryClickListener(category)
            }
        }
    }
}
