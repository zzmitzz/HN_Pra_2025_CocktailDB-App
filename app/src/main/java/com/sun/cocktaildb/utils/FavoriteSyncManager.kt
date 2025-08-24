package com.sun.cocktaildb.utils

import android.os.Handler
import android.os.Looper
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.impl.CocktailRepositoryImpl
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Manager to handle favorite synchronization across all screens
 * Ensures all screens (Home, Search, Favorite, Detail) stay in sync when favorites change
 */
object FavoriteSyncManager {
    private val repository = CocktailRepositoryImpl()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    interface FavoriteUpdateListener {
        fun onFavoriteUpdated(cocktailId: String, isFavorite: Boolean)
        fun onFavoritesRefreshed()
    }

    private val listeners = mutableListOf<FavoriteUpdateListener>()

    fun registerListener(listener: FavoriteUpdateListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun unregisterListener(listener: FavoriteUpdateListener) {
        listeners.remove(listener)
    }

    /**
     * Update favorite status and notify all screens
     * This is the main method called when user clicks favorite button
     */
    fun updateFavorite(cocktail: Cocktail, isFavorite: Boolean) {
        // Update local FavoriteManager immediately for UI responsiveness
        if (isFavorite) {
            FavoriteManager.addToFavorites(cocktail)
        } else {
            FavoriteManager.removeFromFavorites(cocktail)
        }
        
        // Save to Firebase and notify all screens
        executor.execute {
            try {
                if (isFavorite) {
                    repository.addFavourite(cocktail.id)
                } else {
                    repository.removeFavourite(cocktail.id)
                }
                
                // Notify all registered screens about the specific cocktail update
                mainHandler.post {
                    notifyListeners(cocktail.id, isFavorite)
                }
            } catch (e: Exception) {
                // If Firebase fails, still notify local screens
                mainHandler.post {
                    notifyListeners(cocktail.id, isFavorite)
                }
            }
        }
    }

    fun refreshAllScreens() {
        executor.execute {
            try {
                repository.getFavouriteCocktails { result ->
                    if (result.isSuccess) {
                        val favoriteCocktails = result.getOrNull() ?: emptyList()
                        FavoriteManager.clearFavorites()
                        favoriteCocktails.forEach { cocktail ->
                            FavoriteManager.addToFavorites(cocktail)
                        }
                        mainHandler.post {
                            notifyAllScreensRefreshed()
                        }
                    } else {
                        mainHandler.post {
                            notifyAllScreensRefreshed() // Still notify screens to refresh from local data
                        }
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    notifyAllScreensRefreshed() // Handle error silently and notify screens to refresh from local data
                }
            }
        }
    }

    fun isFavorite(cocktailId: String): Boolean {
        return FavoriteManager.isFavorite(cocktailId)
    }

    fun getFavoriteCocktails(): List<Cocktail> {
        return emptyList() // Placeholder, as FavoriteManager only stores IDs
    }

    fun getFavoriteCocktailIds(): Set<String> {
        return FavoriteManager.getFavoriteCocktails()
    }

    private fun notifyListeners(cocktailId: String, isFavorite: Boolean) {
        println("FavoriteSyncManager: Notifying ${listeners.size} listeners for $cocktailId, isFavorite: $isFavorite")
        listeners.forEach { listener ->
            try {
                listener.onFavoriteUpdated(cocktailId, isFavorite)
            } catch (e: Exception) {
                println("FavoriteSyncManager: Error notifying listener: ${e.message}")
                listeners.remove(listener)
            }
        }
    }

    private fun notifyAllScreensRefreshed() {
        listeners.forEach { listener ->
            try {
                listener.onFavoritesRefreshed()
            } catch (e: Exception) {
                listeners.remove(listener)
            }
        }
    }
}
