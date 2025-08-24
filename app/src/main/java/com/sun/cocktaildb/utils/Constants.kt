package com.sun.cocktaildb.utils

object Constants {
    // API and URLs
    const val BASE_URL = "https://www.thecocktaildb.com/api/json/v1/1/"
    const val PLACEHOLDER_IMAGE_URL = "https://example.com/placeholder.jpg"
    const val CATEGORY_IMAGE_BASE_URL = "https://example.com"
    
    // Default values
    const val DEFAULT_CATEGORY = "Unknown"
    const val DEFAULT_INSTRUCTIONS = "Instructions not available"
    const val DEFAULT_INGREDIENTS = "Ingredients not available"
    const val DEFAULT_DESCRIPTION = "Delicious cocktail"
    
    // Error messages
    const val UNKNOWN_ERROR = "Unknown error"
    const val ERROR_LOADING_CATEGORIES = "Error loading categories"
    const val ERROR_LOADING_POPULAR_COCKTAILS = "Error loading popular cocktails"
    const val ERROR_UPDATING_FAVORITE = "Error updating favorite"
}
