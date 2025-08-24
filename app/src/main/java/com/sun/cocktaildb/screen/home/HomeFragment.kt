package com.sun.cocktaildb.screen.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.sun.cocktaildb.R
import com.sun.cocktaildb.data.model.Category
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.impl.CocktailRepositoryImpl
import com.sun.cocktaildb.databinding.FragmentHomeBinding
import com.sun.cocktaildb.screen.categorydetail.CategoryDetailActivity
import com.sun.cocktaildb.screen.cocktaildetail.CocktailActivity
import com.sun.cocktaildb.screen.home.adapter.CategoryAdapter
import com.sun.cocktaildb.screen.home.adapter.PopularCocktailAdapter
import com.sun.cocktaildb.utils.FavoriteManager
import com.sun.cocktaildb.utils.FavoriteSyncManager
import com.sun.cocktaildb.utils.base.BaseFragment
import com.sun.cocktaildb.utils.dialog.LoadingDialog

class HomeFragment :
    BaseFragment(),
    HomeView,
    FavoriteSyncManager.FavoriteUpdateListener {
    private val binding: FragmentHomeBinding by lazy {
        FragmentHomeBinding.inflate(layoutInflater)
    }
    private lateinit var presenter: HomePresenter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var popularCocktailAdapter: PopularCocktailAdapter

    private val loadingDialog by lazy {
        LoadingDialog(this@HomeFragment.requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = binding.root

    override fun initView() {
        setupPresenter()
        setupRecyclerViews()
        // Register for favorite updates
        FavoriteSyncManager.registerListener(this)
    }

    private fun setupPresenter() {
        presenter = HomePresenter(CocktailRepositoryImpl())
        presenter.setView(this)
    }

    private fun setupRecyclerViews() {
        // Setup Categories RecyclerView
        categoryAdapter =
            CategoryAdapter { category ->
                presenter.onCategoryClicked(category)
            }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // Setup Popular Cocktails RecyclerView
        popularCocktailAdapter =
            PopularCocktailAdapter(
                onCocktailClickListener = { cocktail ->
                    presenter.onCocktailClicked(cocktail)
                },
                onFavoriteClickListener = { cocktail, isFavorite ->
                    presenter.onFavoriteClicked(cocktail, isFavorite)
                },
            )
        binding.rvPopular.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = popularCocktailAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.onStart()
        // Refresh popular cocktails to sync with changes from other screens
        refreshPopularCocktailsFromOtherScreens()
    }

    override fun onPause() {
        super.onPause()
        presenter.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister from favorite updates
        FavoriteSyncManager.unregisterListener(this)
    }

    // FavoriteSyncManager.FavoriteUpdateListener implementations
    override fun onFavoriteUpdated(cocktailId: String, isFavorite: Boolean) {
        // Update the specific cocktail's favorite status in the adapter
        popularCocktailAdapter.updateCocktailFavoriteStatus(cocktailId, isFavorite)
    }

    override fun onFavoritesRefreshed() {
        // Refresh all popular cocktails with current favorite status
        refreshPopularCocktailsFromOtherScreens()
    }

    private fun refreshPopularCocktailsFromOtherScreens() {
        // Get current popular cocktails and update their favorite status
        val currentCocktails = popularCocktailAdapter.getCurrentCocktails()
        if (currentCocktails.isNotEmpty()) {
            // Update favorite status for each cocktail based on FavoriteManager
            currentCocktails.forEach { cocktail ->
                val isFavorite = FavoriteSyncManager.isFavorite(cocktail.id)
                popularCocktailAdapter.updateCocktailFavoriteStatus(cocktail.id, isFavorite)
            }
        }
    }

    // HomeView implementations
    override fun showCategories(categories: List<Category>) {
        categoryAdapter.updateCategories(categories)
    }

    override fun showPopularCocktails(cocktails: List<Cocktail>) {
        popularCocktailAdapter.updateCocktails(cocktails)
    }

    override fun showLoading() {
        loadingDialog.show()
    }

    override fun hideLoading() {
        loadingDialog.hide()
    }

    override fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onCategoryClicked(category: Category) {
        val intent = CategoryDetailActivity.newIntent(requireContext(), category)
        startActivity(intent)
        Toast.makeText(context, getString(R.string.navigate_to_category_screen, category.name), Toast.LENGTH_SHORT).show()
    }

    override fun onCocktailClicked(cocktail: Cocktail) {
        val intent = CocktailActivity.newIntent(requireContext(), cocktail.id)
        startActivity(intent)
    }

    override fun onFavoriteClicked(
        cocktail: Cocktail,
        isFavorite: Boolean,
    ) {
        // Show toast message for user feedback
        if (isFavorite) {
            Toast.makeText(context, getString(R.string.added_to_favorites, cocktail.name), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, getString(R.string.removed_from_favorites, cocktail.name), Toast.LENGTH_SHORT).show()
        }

        // Update the cocktail favorite status in the adapter immediately for UI responsiveness
        popularCocktailAdapter.updateCocktailFavoriteStatus(cocktail.id, isFavorite)

        // Use FavoriteSyncManager to update and notify all screens
        // This will:
        // 1. Update local FavoriteManager
        // 2. Save to Firebase
        // 3. Notify Search, Favorite, and Detail screens
        FavoriteSyncManager.updateFavorite(cocktail, isFavorite)

        // Update favorites count in bottom navigation if available
        updateFavoritesCount()
    }

    private fun updateFavoritesCount() {
        val favoritesCount = FavoriteSyncManager.getFavoriteCocktails().size
        // TODO: Update badge count in bottom navigation if supported
    }

    override fun onBottomNavigationItemSelected(itemId: Int) {
        // Navigation is handled by Activity
    }
}
