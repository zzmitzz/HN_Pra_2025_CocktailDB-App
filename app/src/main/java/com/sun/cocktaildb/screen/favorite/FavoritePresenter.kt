package com.sun.cocktaildb.screen.favorite

import android.os.Handler
import android.os.Looper
import com.sun.cocktaildb.data.repository.impl.CocktailRepositoryImpl
import com.sun.cocktaildb.utils.base.BasePresenter

class FavoritePresenter(
    private val repository: CocktailRepositoryImpl = CocktailRepositoryImpl(),
) : BasePresenter<FavoriteView> {
    private var view: FavoriteView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun setView(view: FavoriteView?) {
        this.view = view
    }

    override fun onStart() {
        loadFavorites()
    }

    override fun onStop() { /* no-op */ }

    private fun loadFavorites() {
        view?.showLoading()
        repository.getFavouriteCocktails { result ->
            mainHandler.post {
                view?.hideLoading()
                if (result.isSuccess) {
                    val items = result.getOrNull().orEmpty()
                    view?.showFavorites(items)
                } else {
                    view?.showFavorites(emptyList())
                    view?.showError(result.exceptionOrNull()?.message ?: "Failed to load favorites")
                }
            }
        }
    }
}
