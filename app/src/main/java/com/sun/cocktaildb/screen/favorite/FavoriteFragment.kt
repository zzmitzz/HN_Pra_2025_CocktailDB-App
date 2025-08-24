package com.sun.cocktaildb.screen.favorite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sun.cocktaildb.R
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.impl.CocktailRepositoryImpl
import com.sun.cocktaildb.databinding.FragmentFavoriteBinding
import com.sun.cocktaildb.screen.cocktaildetail.CocktailActivity
import com.sun.cocktaildb.screen.favorite.adapter.FavoriteAdapter
import com.sun.cocktaildb.utils.FavoriteSyncManager
import com.sun.cocktaildb.utils.base.BaseFragment
import com.sun.cocktaildb.utils.dialog.LoadingDialog

class FavoriteFragment : BaseFragment(), FavoriteView, FavoriteSyncManager.FavoriteUpdateListener {
    private val binding: FragmentFavoriteBinding by lazy {
        FragmentFavoriteBinding.inflate(layoutInflater)
    }
    private lateinit var presenter: FavoritePresenter
    private lateinit var favoriteAdapter: FavoriteAdapter

    private val loadingDialog by lazy {
        LoadingDialog(this@FavoriteFragment.requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = binding.root

    override fun initView() {
        setupPresenter()
        setupRecyclerView()
        // Register for favorite updates
        FavoriteSyncManager.registerListener(this)
    }

    private fun setupPresenter() {
        presenter = FavoritePresenter(CocktailRepositoryImpl())
        presenter.setView(this)
    }

    private fun setupRecyclerView() {
        favoriteAdapter = FavoriteAdapter(
            onCocktailClickListener = { cocktail ->
                val intent = CocktailActivity.newIntent(requireContext(), cocktail.id)
                startActivity(intent)
            }
        )
        binding.rvFavorites.apply {
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)
            adapter = favoriteAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorites list when returning from detail screen or search screen
        presenter.onStart()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister from favorite updates
        FavoriteSyncManager.unregisterListener(this)
    }

    // FavoriteSyncManager.FavoriteUpdateListener implementations
    override fun onFavoriteUpdated(cocktailId: String, isFavorite: Boolean) {
        // Refresh the entire list when any favorite status changes
        // This ensures the favorites list is always up to date
        presenter.onStart()
    }

    override fun onFavoritesRefreshed() {
        // Refresh the entire favorites list when any screen updates favorites
        // This ensures the favorites list is always up to date
        presenter.onStart()
    }

    // FavoriteView implementations
    override fun showFavorites(items: List<Cocktail>) {
        if (items.isNotEmpty()) {
            binding.emptyStateContainer.visibility = View.GONE
            binding.rvFavorites.visibility = View.VISIBLE
            favoriteAdapter.updateCocktails(items)
        } else {
            binding.emptyStateContainer.visibility = View.VISIBLE
            binding.rvFavorites.visibility = View.GONE
        }
    }

    override fun showLoading() {
        loadingDialog.show()
    }

    override fun hideLoading() {
        loadingDialog.hide()
    }

    override fun showError(message: String) {
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.rvFavorites.visibility = View.GONE
        // You can also show a toast or snackbar here
    }
}


