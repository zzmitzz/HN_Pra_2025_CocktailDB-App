package com.sun.cocktaildb.screen.cocktaildetail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sun.cocktaildb.databinding.ItemIngredientBinding

class IngredientAdapter : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {
    private var ingredients: List<String> = emptyList()

    fun updateIngredients(newIngredients: List<String>) {
        ingredients = newIngredients
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): IngredientViewHolder {
        val binding =
            ItemIngredientBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return IngredientViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: IngredientViewHolder,
        position: Int,
    ) {
        holder.bind(ingredients[position])
    }

    override fun getItemCount(): Int = ingredients.size

    inner class IngredientViewHolder(
        private val binding: ItemIngredientBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ingredient: String) {
            binding.apply {
                // Expected formats:
                // 1) "IngredientName|||Measure"
                // 2) "IngredientName" (no measure)
                val raw = ingredient.trim()
                if (raw.contains("|||")) {
                    val (name, measure) = raw.split("|||", limit = 2)
                    tvIngredient.text = name
                    if (measure.isNotBlank()) {
                        tvMeasure.text = measure
                        tvMeasure.visibility = android.view.View.VISIBLE
                    } else {
                        tvMeasure.text = ""
                        tvMeasure.visibility = android.view.View.GONE
                    }
                } else {
                    tvIngredient.text = raw
                    tvMeasure.text = ""
                    tvMeasure.visibility = android.view.View.GONE
                }
            }
        }
    }
} 
