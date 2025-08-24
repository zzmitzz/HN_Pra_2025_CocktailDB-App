package com.sun.cocktaildb.screen.search

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.impl.CocktailRepositoryImpl

/**
 * Manages search functionality logic, separated from UI
 */
class SearchFragmentManager(
    private val context: Context,
    private val onSearchResults: (List<Cocktail>) -> Unit,
    private val onNoResults: () -> Unit,
    private val onError: (String) -> Unit
) {
    
    private lateinit var searchPresenter: SearchPresenter
    
    fun initialize() {
        if (!::searchPresenter.isInitialized) {
            searchPresenter = SearchPresenter(CocktailRepositoryImpl())
            searchPresenter.setView(object : SearchView {
                override fun showSearchResults(cocktails: List<Cocktail>) {
                    onSearchResults(cocktails)
                }
                
                override fun showNoResults() {
                    onNoResults()
                }
                
                override fun showLoading() {
                    // Loading will be handled by fragment
                }
                
                override fun hideLoading() {
                    // Loading will be handled by fragment
                }
                
                override fun clearSearchResults() {
                    onNoResults()
                }
                
                override fun onCocktailClicked(cocktail: Cocktail) {
                    // Click will be handled by fragment
                }
                
                override fun showError(message: String) {
                    onError(message)
                }
                
                override fun showHistory(historyItems: List<String>) {
                    // History will be handled by fragment
                }
                
                override fun hideHistory() {
                    // History will be handled by fragment
                }
                
                override fun removeFromHistory(query: String) {
                    // History removal will be handled by fragment
                }
                
                override fun showAlcoholicFilters(filters: List<String>) {
                    // Already handled in setupAlcoholicFilterDropdown
                }
                
                override fun showGlassTypes(glassTypes: List<String>) {
                    // TODO: Implement glass type filter
                }
                
                override fun showSearchTypeTabs() {
                    // Fragment already handles tabs
                }
                
                override fun updateSearchTypeTab(selectedType: SearchType) {
                    // Fragment already handles tabs
                }
                
                override fun showQuickFilters() {
                    // Fragment already handles filters
                }
                
                override fun showFilterOptions() {
                    // TODO: Implement advanced filter options
                }
            })
            searchPresenter.onStart()
        }
    }
    
    fun getSearchPresenter(): SearchPresenter {
        return searchPresenter
    }
    
    // Create adapter for alcoholic filter
    fun createAlcoholicFilterAdapter(): ArrayAdapter<String> {
        val alcoholicFilters = arrayOf("All", "Alcoholic", "Non_Alcoholic", "Optional_Alcohol")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, alcoholicFilters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }
    
    // Create listener for alcoholic filter
    fun createAlcoholicFilterListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val filter = when (position) {
                    0 -> null
                    1 -> "Alcoholic"
                    2 -> "Non_Alcoholic"
                    3 -> "Optional_Alcohol"
                    else -> null
                }
                searchPresenter.setAlcoholicFilter(filter)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    // Create adapter for ingredient filter
    fun createIngredientFilterAdapter(): ArrayAdapter<String> {
        val ingredientFilters = arrayOf("All", "Gin", "Vodka", "Rum", "Tequila", "Whiskey", "Brandy", "Liqueur")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, ingredientFilters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }
    
    // Create listener for ingredient filter
    fun createIngredientFilterListener(): AdapterView.OnItemSelectedListener {
        val ingredientFilters = arrayOf("All", "Gin", "Vodka", "Rum", "Tequila", "Whiskey", "Brandy", "Liqueur")
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val filter = when (position) {
                    0 -> null
                    else -> ingredientFilters[position]
                }
                searchPresenter.setIngredientFilter(filter)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    // Search functionality
    fun searchCocktails(query: String, searchType: SearchType) {
        searchPresenter.searchCocktails(query, searchType)
    }
    
    fun setSearchType(searchType: SearchType) {
        searchPresenter.setSearchType(searchType)
    }
    
    fun getCurrentSearchType(): SearchType {
        return searchPresenter.getCurrentSearchType()
    }
    
    fun clearSearchResults() {
        searchPresenter.clearSearchResults()
    }
    
    fun addToHistory(query: String) {
        searchPresenter.addToHistory(query)
    }
    
    fun removeFromHistory(query: String) {
        searchPresenter.removeFromHistory(query)
    }
}
