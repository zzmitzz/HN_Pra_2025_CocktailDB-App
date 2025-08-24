package com.sun.cocktaildb.screen.categorydetail

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.GridLayoutManager
import com.sun.cocktaildb.R
import com.sun.cocktaildb.data.model.Category
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.impl.CocktailRepositoryImpl
import com.sun.cocktaildb.databinding.ActivityCategoryDetailBinding
import com.sun.cocktaildb.screen.cocktaildetail.CocktailActivity
import com.sun.cocktaildb.screen.home.adapter.PopularCocktailAdapter
import com.sun.cocktaildb.utils.FavoriteManager
import com.sun.cocktaildb.utils.base.BaseActivity

class CategoryDetailActivity :
    BaseActivity(),
    CategoryDetailView {
    private lateinit var binding: ActivityCategoryDetailBinding
    private lateinit var presenter: CategoryDetailPresenter
    private lateinit var cocktailAdapter: PopularCocktailAdapter

    companion object {
        private const val EXTRA_CATEGORY = "extra_category"

        fun newIntent(
            context: Context,
            category: Category,
        ): Intent =
            Intent(context, CategoryDetailActivity::class.java).apply {
                putExtra(EXTRA_CATEGORY, category)
            }
    }

    override fun initView() {
        binding = ActivityCategoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val category =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_CATEGORY, Category::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Category>(EXTRA_CATEGORY)
            } ?: throw IllegalArgumentException("Category is required")

        setupPresenter(category)
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupPresenter(category: Category) {
        presenter = CategoryDetailPresenter(CocktailRepositoryImpl(), category)
        presenter.setView(this)
    }

    private fun setupRecyclerView() {
        cocktailAdapter =
            PopularCocktailAdapter(
                onCocktailClickListener = { cocktail ->
                    presenter.onCocktailClicked(cocktail)
                },
                onFavoriteClickListener = { cocktail, isFavorite ->
                    presenter.onFavoriteClicked(cocktail, isFavorite)
                },
            )
        binding.rvCocktails.apply {
            layoutManager = GridLayoutManager(this@CategoryDetailActivity, 2)
            adapter = cocktailAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.onStart()
    }

    override fun onPause() {
        super.onPause()
        presenter.onStop()
    }

    // CategoryDetailView implementations
    override fun showCategory(category: Category) {
        binding.tvCategoryTitle.text = category.name
    }

    override fun showCocktails(cocktails: List<Cocktail>) {
        cocktailAdapter.updateCocktails(cocktails)
    }

    override fun showLoading() {
        binding.progressBar.visibility = android.view.View.VISIBLE
    }

    override fun hideLoading() {
        binding.progressBar.visibility = android.view.View.GONE
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCocktailClicked(cocktail: Cocktail) {
        val intent = CocktailActivity.newIntent(this, cocktail.id)
        startActivity(intent)
    }

    override fun onFavoriteClicked(
        cocktail: Cocktail,
        isFavorite: Boolean,
    ) {
        if (isFavorite) {
            Toast.makeText(this, getString(R.string.added_to_favorites, cocktail.name), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.removed_from_favorites, cocktail.name), Toast.LENGTH_SHORT).show()
        }

        // Update the cocktail favorite status in the adapter
        cocktailAdapter.updateCocktailFavoriteStatus(cocktail.id, isFavorite)

        // Update favorite status in FavoriteManager
        if (isFavorite) {
            FavoriteManager.addToFavorites(cocktail)
        } else {
            FavoriteManager.removeFromFavorites(cocktail)
        }
    }

    override fun navigateBack() {
        finish()
    }
}
