package com.sun.cocktaildb.screen.search

import android.os.Handler
import android.os.Looper
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.remote.CocktailRepository
import com.sun.cocktaildb.utils.base.BasePresenter
import com.sun.cocktaildb.utils.FavoriteManager
import java.util.concurrent.Executors

class SearchPresenter(
    private val cocktailRepository: CocktailRepository,
) : BasePresenter<SearchView> {
    private var view: SearchView? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Search state tracking
    private var currentSearchType = SearchType.NAME
    private var currentQuery = ""

    // Filter state
    private var selectedAlcoholicFilter: String? = null
    private var selectedIngredientFilter: String? = null

    // Search history
    private val searchHistory = mutableListOf<String>()
    private val maxHistorySize = 10

    override fun setView(view: SearchView?) {
        this.view = view
    }

    override fun onStart() {
        // Show search history when starting
        view?.showHistory(searchHistory)
    }

    override fun onStop() {
        // Clean up resources
    }

    // Search cocktails with query and search type
    fun searchCocktails(
        query: String,
        searchType: SearchType,
    ) {
        currentSearchType = searchType
        currentQuery = query.trim()

        val hasQuery = query.trim().isNotEmpty()
        val hasFilters = selectedAlcoholicFilter != null || selectedIngredientFilter != null
		
        if (!hasQuery && !hasFilters) {
            view?.clearSearchResults()
            view?.showHistory(searchHistory)
            return
        }


        view?.hideHistory()
        view?.showLoading()

        executor.execute {
            try {
                var results =
                    when (searchType) {
                        SearchType.NAME -> {
                            if (hasQuery) {
                                val nameResults = cocktailRepository.searchCocktailsByName(query.trim())
                                // If no results from name search, try first letter search
                                if (nameResults.isEmpty()) {
                                    cocktailRepository.searchCocktailsByFirstLetter(query.trim().first().toString())
                                } else {
                                    nameResults
                                }
                            } else {
                                cocktailRepository.searchCocktailsByFirstLetter("M")
                            }
                        }
                        SearchType.INGREDIENT -> {
                            if (hasQuery) {
                                cocktailRepository.searchCocktailsByIngredient(query.trim())
                            } else if (selectedIngredientFilter != null) {
                                cocktailRepository.searchCocktailsByIngredient(selectedIngredientFilter!!)
                            } else {
                                cocktailRepository.searchCocktailsByFirstLetter("M")
                            }
                        }
                        SearchType.FIRST_LETTER -> {
                            if (hasQuery) {
                                cocktailRepository.searchCocktailsByFirstLetter(query.trim())
                            } else {
                                cocktailRepository.searchCocktailsByFirstLetter("M")
                            }
                        }
                    }

                // Apply filters if any
                results = applyFilters(results)

                // Sort results by search priority (left to right matching)
                results = sortResultsBySearchPriority(results, query.trim())

                // Update favorite status from Firebase
                updateFavoriteStatus(results)

            } catch (e: Exception) {
                mainHandler.post {
                    view?.hideLoading()
                    view?.showError("Search failed: ${e.message}")
                }
            }
        }
    }

    private fun applyFilters(results: List<Cocktail>): List<Cocktail> {
        var filteredResults = results

        // Apply alcoholic filter
        selectedAlcoholicFilter?.let { filter ->
            if (filter != "All") {
                filteredResults = filteredResults.filter { cocktail ->
                    cocktail.description.contains(filter, ignoreCase = true)
                }
            }
        }

        // Apply ingredient filter
        selectedIngredientFilter?.let { filter ->
            if (filter != "All") {
                filteredResults = filteredResults.filter { cocktail ->
                    cocktail.ingredients.any { ingredient ->
                        ingredient.contains(filter, ignoreCase = true)
                    }
                }
            }
        }

        return filteredResults
    }

    private fun sortResultsBySearchPriority(results: List<Cocktail>, query: String): List<Cocktail> {
        if (query.isEmpty()) return results
        
        return results.sortedBy { cocktail ->
            val nameLower = cocktail.name.lowercase()
            val queryLower = query.lowercase()
            
            // Find the first occurrence of the query in the cocktail name
            val firstIndex = nameLower.indexOf(queryLower)
            
            // If not found, put at the end
            if (firstIndex == -1) {
                Int.MAX_VALUE
            } else {
                // Return the position (lower position = higher priority)
                firstIndex
            }
        }
    }

    private fun updateFavoriteStatus(results: List<Cocktail>) {
        cocktailRepository.getFavouriteCocktails { result ->
            if (result.isSuccess) {
                val favoriteIds = result.getOrNull()?.map { it.id } ?: emptyList()
                
                // Update cocktails with favorite status from Firebase
                val updatedResults = results.map { cocktail ->
                    val isFavorite = favoriteIds.contains(cocktail.id)
                    cocktail.copy(isFavorite = isFavorite)
                }
                
                // Update local FavoriteManager for consistency
                updatedResults.forEach { cocktail ->
                    if (cocktail.isFavorite) {
                        FavoriteManager.addToFavorites(cocktail)
                    } else {
                        FavoriteManager.removeFromFavorites(cocktail)
                    }
                }

                mainHandler.post {
                    view?.hideLoading()
                    if (updatedResults.isNotEmpty()) {
                        view?.showSearchResults(updatedResults)
                    } else {
                        view?.showNoResults()
                    }
                }
            } else {
                // Fallback to local FavoriteManager if Firebase fails
                val updatedResults = results.map { cocktail ->
                    cocktail.copy(isFavorite = FavoriteManager.isFavorite(cocktail.id))
                }
                
                mainHandler.post {
                    view?.hideLoading()
                    if (updatedResults.isNotEmpty()) {
                        view?.showSearchResults(updatedResults)
                    } else {
                        view?.showNoResults()
                    }
                }
            }
        }
    }

    // Set search type
    fun setSearchType(searchType: SearchType) {
        currentSearchType = searchType
    }

    // Get current search type
    fun getCurrentSearchType(): SearchType = currentSearchType

    // Clear search results and show history
    fun clearSearchResults() {
        currentQuery = ""
        selectedAlcoholicFilter = null
        selectedIngredientFilter = null
        view?.clearSearchResults()
        view?.showHistory(searchHistory)
    }

    // Set alcoholic filter
    fun setAlcoholicFilter(filter: String?) {
        selectedAlcoholicFilter = filter
        selectedIngredientFilter = null
    }

    // Set ingredient filter
    fun setIngredientFilter(filter: String?) {
        selectedIngredientFilter = filter
        selectedAlcoholicFilter = null
    }

    // Add query to search history
    fun addToHistory(query: String) {
        searchHistory.remove(query)
        searchHistory.add(0, query)
        if (searchHistory.size > maxHistorySize) {
            searchHistory.removeAt(searchHistory.size - 1)
        }
    }

    // Remove query from search history
    fun removeFromHistory(query: String) {
        searchHistory.remove(query)
        view?.showHistory(searchHistory)
    }

    // Get search history
    fun getSearchHistory(): List<String> = searchHistory.toList()

    // Clear search history
    fun clearSearchHistory() {
        searchHistory.clear()
        view?.showHistory(searchHistory)
    }

    // Get current query
    fun getCurrentQuery(): String = currentQuery

    // Get current filters
    fun getCurrentFilters(): Pair<String?, String?> = Pair(selectedAlcoholicFilter, selectedIngredientFilter)

    // Check if search is active
    fun isSearchActive(): Boolean = currentQuery.isNotEmpty() || selectedAlcoholicFilter != null || selectedIngredientFilter != null

    // Reset search state
    fun resetSearch() {
        currentQuery = ""
        selectedAlcoholicFilter = null
        selectedIngredientFilter = null
        view?.clearSearchResults()
        view?.showHistory(searchHistory)
    }
}

enum class SearchType {
    NAME,
    INGREDIENT,
    FIRST_LETTER,
}
