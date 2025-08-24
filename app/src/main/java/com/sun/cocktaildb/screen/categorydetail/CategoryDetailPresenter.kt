package com.sun.cocktaildb.screen.categorydetail

import android.os.Handler
import android.os.Looper
import com.sun.cocktaildb.data.model.Category
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.remote.CocktailRepository
import com.sun.cocktaildb.utils.Constants
import com.sun.cocktaildb.utils.FavoriteManager
import com.sun.cocktaildb.utils.base.BasePresenter
import java.util.concurrent.Executors

class CategoryDetailPresenter(
    private val cocktailRepository: CocktailRepository,
    private val category: Category,
) : BasePresenter<CategoryDetailView> {
    private var view: CategoryDetailView? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun setView(view: CategoryDetailView?) {
        this.view = view
    }

    override fun onStart() {
        view?.showCategory(category)
        loadCocktailsByCategory()
    }

    override fun onStop() {
        // Cleanup if needed
    }

    private fun loadCocktailsByCategory() {
        view?.showLoading()
        executor.execute {
            try {
                val cocktails = cocktailRepository.getCocktailsByCategory(category.id)
                // Update favorite status based on FavoriteManager
                val updatedCocktails =
                    cocktails.map { cocktail ->
                        cocktail.copy(isFavorite = FavoriteManager.isFavorite(cocktail.id))
                    }
                mainHandler.post {
                    view?.showCocktails(updatedCocktails)
                    view?.hideLoading()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    view?.showError(e.message ?: "Error loading cocktails")
                    view?.hideLoading()
                }
            }
        }
    }

    fun onCocktailClicked(cocktail: Cocktail) {
        view?.onCocktailClicked(cocktail)
    }

    fun onFavoriteClicked(
        cocktail: Cocktail,
        isFavorite: Boolean,
    ) {
        view?.onFavoriteClicked(cocktail, isFavorite)
        executor.execute {
            try {
                if (isFavorite) {
                    cocktailRepository.addFavourite(cocktail.id)
                } else {
                    cocktailRepository.removeFavourite(cocktail.id)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    view?.showError("${Constants.ERROR_UPDATING_FAVORITE}: ${e.message ?: Constants.UNKNOWN_ERROR}")
                }
            }
        }
    }

    fun onBackClicked() {
        view?.navigateBack()
    }
}
