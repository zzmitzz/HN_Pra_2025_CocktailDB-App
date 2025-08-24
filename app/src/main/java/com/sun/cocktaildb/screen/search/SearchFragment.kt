package com.sun.cocktaildb.screen.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.sun.cocktaildb.R
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.impl.CocktailRepositoryImpl
import com.sun.cocktaildb.databinding.FragmentSearchBinding
import com.sun.cocktaildb.screen.cocktaildetail.CocktailActivity
import com.sun.cocktaildb.screen.search.adapter.HistoryAdapter
import com.sun.cocktaildb.screen.search.adapter.SearchAdapter
import com.sun.cocktaildb.utils.FavoriteSyncManager
import com.sun.cocktaildb.utils.base.BaseFragment
import com.sun.cocktaildb.utils.dialog.LoadingDialog

class SearchFragment : BaseFragment(), SearchView, FavoriteSyncManager.FavoriteUpdateListener {
    private val binding: FragmentSearchBinding by lazy {
        FragmentSearchBinding.inflate(layoutInflater)
    }
    private lateinit var presenter: SearchPresenter
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var historyAdapter: HistoryAdapter

    private val loadingDialog by lazy {
        LoadingDialog(this@SearchFragment.requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = binding.root

    override fun initView() {
        setupPresenter()
        setupRecyclerViews()
        setupSearchInput()
        setupSearchTabs()
        setupQuickFilters()
        setupSearchButton()
        FavoriteSyncManager.registerListener(this)
    }

    private fun setupPresenter() {
        presenter = SearchPresenter(CocktailRepositoryImpl())
        presenter.setView(this)
    }

    private fun setupRecyclerViews() {
        // Setup Search Results RecyclerView
        searchAdapter = SearchAdapter(
            onCocktailClicked = { cocktail ->
                val intent = CocktailActivity.newIntent(requireContext(), cocktail.id)
                startActivity(intent)
            },
            onFavoriteClickListener = { cocktail, isFavorite ->
                onFavoriteClicked(cocktail, isFavorite)
            }
        )
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
            setHasFixedSize(false)
        }

        // Setup History RecyclerView
        historyAdapter = HistoryAdapter(
            onHistoryItemClickListener = { query ->
                binding.etSearch.setText(query)
                // Auto search when clicking history item
                presenter.searchCocktails(query, presenter.getCurrentSearchType())
            },
            onHistoryItemDeleteClickListener = { query ->
                presenter.removeFromHistory(query)
            }
        )
        binding.rvSearchHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun setupSearchInput() {
        // Remove real-time search - only search when button is clicked
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                if (query.isEmpty()) {
                    presenter.clearSearchResults()
                    showSearchHistory()
                }
            }
        })
    }

    private fun setupSearchTabs() {
        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.position) {
                        0 -> {
                            presenter.setSearchType(SearchType.NAME)
                            binding.etSearch.hint = getString(R.string.search_hint_name)
                            showAlcoholicFilterOnly()
                            presenter.clearSearchResults()
                            showSearchHistory()
                        }
                        1 -> {
                            presenter.setSearchType(SearchType.INGREDIENT)
                            binding.etSearch.hint = getString(R.string.search_hint_ingredient)
                            showIngredientFilterOnly()
                            presenter.clearSearchResults()
                            showSearchHistory()
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            },
        )
    }

    private fun setupQuickFilters() {
        setupAlcoholicFilterDropdown()
        setupIngredientFilterDropdown()
        showAlcoholicFilterOnly()
    }

    private fun setupAlcoholicFilterDropdown() {
        val alcoholicFilters = arrayOf("All", "Alcoholic", "Non_Alcoholic", "Optional_Alcohol")
        val alcoholicAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, alcoholicFilters)
        alcoholicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.alcoholicFilterSpinner.adapter = alcoholicAdapter

        binding.alcoholicFilterSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val filter = when (position) {
                    0 -> null
                    1 -> "Alcoholic"
                    2 -> "Non_Alcoholic"
                    3 -> "Optional_Alcohol"
                    else -> null
                }
                presenter.setAlcoholicFilter(filter)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun setupIngredientFilterDropdown() {
        val ingredientFilters = arrayOf("All", "Gin", "Vodka", "Rum", "Tequila", "Whiskey", "Brandy", "Liqueur")
        val ingredientAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ingredientFilters)
        ingredientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.ingredientFilterSpinner.adapter = ingredientAdapter

        binding.ingredientFilterSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val filter = when (position) {
                    0 -> null
                    else -> ingredientFilters[position]
                }
                presenter.setIngredientFilter(filter)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun showAlcoholicFilterOnly() {
        binding.alcoholicFilterSpinner.visibility = View.VISIBLE
        binding.ingredientFilterSpinner.visibility = View.GONE
    }

    private fun showIngredientFilterOnly() {
        binding.alcoholicFilterSpinner.visibility = View.GONE
        binding.ingredientFilterSpinner.visibility = View.VISIBLE
    }

    private fun setupSearchButton() {
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                presenter.searchCocktails(query, presenter.getCurrentSearchType())
                // Add to history when search button is clicked
                presenter.addToHistory(query)
            } else {
                Toast.makeText(context, getString(R.string.enter_search_query), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showSearchHistory()
        refreshFavoriteStatusFromDetailScreen()
    }

    private fun refreshFavoriteStatusFromDetailScreen() {
        val currentCocktails = searchAdapter.getCurrentCocktails()
        if (currentCocktails.isNotEmpty()) {
            currentCocktails.forEach { cocktail ->
                val isFavorite = FavoriteSyncManager.isFavorite(cocktail.id)
                searchAdapter.updateCocktailFavoriteStatus(cocktail.id, isFavorite)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        FavoriteSyncManager.unregisterListener(this)
    }

    // FavoriteSyncManager.FavoriteUpdateListener implementations
    override fun onFavoriteUpdated(cocktailId: String, isFavorite: Boolean) {
        searchAdapter.updateCocktailFavoriteStatus(cocktailId, isFavorite)
    }

    override fun onFavoritesRefreshed() {
        refreshSearchResultsFromOtherScreens()
    }

    private fun refreshSearchResultsFromOtherScreens() {
        val currentCocktails = searchAdapter.getCurrentCocktails()
        if (currentCocktails.isNotEmpty()) {
            currentCocktails.forEach { cocktail ->
                val isFavorite = FavoriteSyncManager.isFavorite(cocktail.id)
                searchAdapter.updateCocktailFavoriteStatus(cocktail.id, isFavorite)
            }
        }
    }

    // SearchView implementations
    override fun showSearchResults(cocktails: List<Cocktail>) {
        binding.rvSearchResults.visibility = View.VISIBLE
        binding.rvSearchHistory.visibility = View.GONE
        binding.llNoResults.visibility = View.GONE
        val currentQuery = binding.etSearch.text.toString().trim()
        searchAdapter.updateCocktails(cocktails, currentQuery)
    }

    override fun showNoResults() {
        binding.rvSearchResults.visibility = View.GONE
        binding.rvSearchHistory.visibility = View.GONE
        binding.llNoResults.visibility = View.VISIBLE
    }

    override fun showHistory(history: List<String>) {
        binding.rvSearchResults.visibility = View.GONE
        binding.rvSearchHistory.visibility = View.VISIBLE
        binding.llNoResults.visibility = View.GONE
        historyAdapter.updateHistory(history)
    }

    override fun hideHistory() {
        binding.rvSearchHistory.visibility = View.GONE
    }

    override fun clearSearchResults() {
        binding.rvSearchResults.visibility = View.GONE
        binding.llNoResults.visibility = View.GONE
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

    override fun onCocktailClicked(cocktail: Cocktail) {
        val intent = CocktailActivity.newIntent(requireContext(), cocktail.id)
        startActivity(intent)
    }

    override fun removeFromHistory(historyItem: String) {
        presenter.removeFromHistory(historyItem)
    }

    // Favorite functionality - KEPT FROM PREVIOUS IMPLEMENTATION
    private fun onFavoriteClicked(cocktail: Cocktail, isFavorite: Boolean) {
        println("SearchFragment: onFavoriteClicked called for ${cocktail.name}, isFavorite: $isFavorite")
        if (isFavorite) {
            Toast.makeText(context, getString(R.string.added_to_favorites, cocktail.name), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, getString(R.string.removed_from_favorites, cocktail.name), Toast.LENGTH_SHORT).show()
        }
        searchAdapter.updateCocktailFavoriteStatus(cocktail.id, isFavorite)
        FavoriteSyncManager.updateFavorite(cocktail, isFavorite)
    }

    override fun showAlcoholicFilters(filters: List<String>) {
        // Implementation for showing alcoholic filters
    }

    override fun showGlassTypes(glassTypes: List<String>) {
        // Implementation for showing glass types
    }

    override fun showSearchTypeTabs() {
        // Implementation for showing search type tabs
    }

    override fun updateSearchTypeTab(selectedType: SearchType) {
        // Implementation for updating search type tab
    }

    override fun showQuickFilters() {
        // Implementation for showing quick filters
    }

    override fun showFilterOptions() {
        // Implementation for showing filter options
    }

    private fun showSearchHistory() {
        val history = presenter.getSearchHistory()
        if (history.isNotEmpty()) {
            showHistory(history)
        }
    }
}
