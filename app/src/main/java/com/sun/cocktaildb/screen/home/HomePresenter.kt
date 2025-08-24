package com.sun.cocktaildb.screen.home

import android.os.Handler
import android.os.Looper
import com.sun.cocktaildb.data.model.Category
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.remote.CocktailRepository
import com.sun.cocktaildb.utils.Constants
import com.sun.cocktaildb.utils.FavoriteManager
import com.sun.cocktaildb.utils.base.BasePresenter
import java.util.concurrent.Executors
import com.sun.cocktaildb.utils.FavoriteSyncManager

class HomePresenter(
    private val cocktailRepository: CocktailRepository,
) : BasePresenter<HomeView> {
    private var view: HomeView? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun setView(view: HomeView?) {
        this.view = view
    }

    override fun onStart() {
        loadCategories()
        loadPopularCocktails()
    }

    override fun onStop() {
        // Cleanup if needed
    }

    private fun loadCategories() {
        view?.showLoading()
        executor.execute {
            try {
                val categories = cocktailRepository.getCategories()
                mainHandler.post {
                    if (categories.isNotEmpty()) {
                        view?.showCategories(categories)
                    } else {
                        view?.showError("No categories found. Please check your internet connection.")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    view?.showError("${Constants.ERROR_LOADING_CATEGORIES}: ${e.message ?: Constants.UNKNOWN_ERROR}")
                }
            }
        }
    }

    private fun loadPopularCocktails() {
        executor.execute {
            try {
                val cocktails = cocktailRepository.getPopularCocktails()
                cocktailRepository.getFavouriteCocktails { result ->
                    val updatedCocktails =
                        if (result.isSuccess) {
                            val favIds = result.getOrNull().orEmpty().map { it.id }.toSet()
                            cocktails.map { it.copy(isFavorite = favIds.contains(it.id)) }
                        } else {
                            cocktails
                        }

                    mainHandler.post {
                        if (updatedCocktails.isNotEmpty()) {
                            view?.showPopularCocktails(updatedCocktails)
                        } else {
                            view?.showError("No popular cocktails found. Please check your internet connection.")
                        }
                        view?.hideLoading()
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    view?.showError("${Constants.ERROR_LOADING_POPULAR_COCKTAILS}: ${e.message ?: Constants.UNKNOWN_ERROR}")
                    view?.hideLoading()
                }
            }
        }
    }

    fun onCategoryClicked(category: Category) {
        view?.onCategoryClicked(category)
    }

    fun onCocktailClicked(cocktail: Cocktail) {
        view?.onCocktailClicked(cocktail)
    }

    fun onFavoriteClicked(
        cocktail: Cocktail,
        isFavorite: Boolean,
    ) {

        // Use FavoriteSyncManager to handle all favorite operations
        // This will automatically update Firebase and notify all screens
        FavoriteSyncManager.updateFavorite(cocktail, isFavorite)

        executor.execute {
            try {
                if (isFavorite) {
                    cocktailRepository.addFavourite(cocktail.id)
                } else {
                    cocktailRepository.removeFavourite(cocktail.id)
                }
                
                // Refresh popular cocktails to show updated favorite status
                loadPopularCocktails()
            } catch (e: Exception) {
                mainHandler.post {
                    view?.showError("${Constants.ERROR_UPDATING_FAVORITE}: ${e.message ?: Constants.UNKNOWN_ERROR}")
                }
            }
        }

    }

    fun onBottomNavigationItemSelected(itemId: Int) {
        view?.onBottomNavigationItemSelected(itemId)
    }
}
