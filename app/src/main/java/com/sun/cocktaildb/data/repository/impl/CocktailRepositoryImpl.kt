package com.sun.cocktaildb.data.repository.impl

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.sun.cocktaildb.data.model.Category
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.remote.CocktailRepository
import com.sun.cocktaildb.utils.Constants
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CocktailRepositoryImpl constructor(
    private val firebase: FirebaseAuthImplement = FirebaseAuthImplement(),
) : CocktailRepository {
    private val baseUrl = Constants.BASE_URL

    // Combine parallel ingredient and measure lists into display items.
    private fun combineIngredientAndMeasure(
        ingredients: List<String>,
        measures: List<String>,
    ): List<String> {
        return ingredients.mapIndexed { index, ingredient ->
            val measure = measures.getOrNull(index).orEmpty().trim()
            val ingredientName = ingredient.trim()
            if (measure.isNotEmpty()) {
                "$ingredientName|||$measure"
            } else {
                ingredientName
            }
        }
    }

    override fun getCategories(): List<Category> {
        try {
            val url = baseUrl + "list.php?c=list"
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<Category>()

            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val name = item?.optString("strCategory").orEmpty()

                if (name.isNotEmpty()) {
                    val sampleCocktails = getCocktailsByCategory(name)
                    val sampleImage =
                        if (sampleCocktails.isNotEmpty()) {
                            sampleCocktails.first().imageUrl
                        } else {
                            "${Constants.CATEGORY_IMAGE_BASE_URL}/$name.jpg"
                        }

                    result.add(
                        Category(
                            id = name,
                            name = name,
                            description = "Various $name types",
                            imageUrl = sampleImage,
                        ),
                    )
                }
            }
            return result
        } catch (e: Exception) {
            return listOf(
                Category("1", "Cocktails", "Classic cocktails", "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg"),
                Category("2", "Beer", "Various beer types", "https://www.thecocktaildb.com/images/media/drink/metwgh1606770327.jpg"),
                Category("3", "Wine", "Red and white wines", "https://www.thecocktaildb.com/images/media/drink/71t8581504353095.jpg"),
                Category("4", "Spirits", "Premium spirits", "https://www.thecocktaildb.com/images/media/drink/4qxyty1434360738.jpg"),
            )
        }
    }

    override fun getPopularCocktails(): List<Cocktail> {
        try {
            // Use search by first letter 'M' to get some popular cocktails
            val url = baseUrl + "search.php?f=M"
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<Cocktail>()

            // Take first 6 cocktails as popular
            val limit = minOf(6, drinksArray.length())

            for (i in 0 until limit) {
                val item = drinksArray.optJSONObject(i)
                val id = item?.optString("idDrink").orEmpty()
                val name = item?.optString("strDrink").orEmpty()
                val thumb = item?.optString("strDrinkThumb").orEmpty()
                val category = item?.optString("strCategory").orEmpty()
                val alcoholic = item?.optString("strAlcoholic").orEmpty()
                val glass = item?.optString("strGlass").orEmpty()
                val tags = item?.optString("strTags").orEmpty()

                if (id.isNotEmpty() && name.isNotEmpty()) {
                    // Extract ingredients (strIngredient1 to strIngredient15)
                    val ingredients = mutableListOf<String>()
                    val measures = mutableListOf<String>()

                    for (j in 1..15) {
                        val ingredient = item.optString("strIngredient$j").orEmpty()
                        val measure = item.optString("strMeasure$j").orEmpty()

                        if (ingredient.isNotEmpty()) {
                            ingredients.add(ingredient)
                            measures.add(measure)
                        }
                    }

                    // Combine ingredients with measures (ingredient first)
                    val ingredientsWithMeasures = combineIngredientAndMeasure(ingredients, measures)

                    val description =
                        buildString {
                            append("Popular cocktail")
                            if (category.isNotEmpty()) append(" • Category: $category")
                            if (alcoholic.isNotEmpty()) append(" • Type: $alcoholic")
                            if (glass.isNotEmpty()) append(" • Glass: $glass")
                            if (tags.isNotEmpty()) append(" • Tags: $tags")
                        }

                    result.add(
                        Cocktail(
                            id = id,
                            name = name,
                            description = description,
                            imageUrl = thumb.ifEmpty { "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg" },
                            ingredients = ingredientsWithMeasures.ifEmpty { listOf(Constants.DEFAULT_INGREDIENTS) },
                            instructions = Constants.DEFAULT_INSTRUCTIONS,
                            category = category.ifEmpty { Constants.DEFAULT_CATEGORY },
                        ),
                    )
                }
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }

    override fun getCocktailsByCategory(categoryId: String): List<Cocktail> {
        try {
            // Treat categoryId as the category name since API uses names
            val encoded = categoryId.replace(' ', '_')
            val url = baseUrl + "filter.php?c=$encoded"
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<Cocktail>()

            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val id = item?.optString("idDrink").orEmpty()
                val name = item?.optString("strDrink").orEmpty()
                val thumb = item?.optString("strDrinkThumb").orEmpty()
                val category = item?.optString("strCategory").orEmpty()
                val alcoholic = item?.optString("strAlcoholic").orEmpty()
                val glass = item?.optString("strGlass").orEmpty()
                val tags = item?.optString("strTags").orEmpty()

                if (id.isNotEmpty() && name.isNotEmpty()) {
                    // Extract ingredients (strIngredient1 to strIngredient15)
                    val ingredients = mutableListOf<String>()
                    for (j in 1..15) {
                        val ingredient = item.optString("strIngredient$j").orEmpty()
                        if (ingredient.isNotEmpty()) {
                            ingredients.add(ingredient)
                        }
                    }

                    // Extract measures (strMeasure1 to strMeasure15)
                    val measures = mutableListOf<String>()
                    for (j in 1..15) {
                        val measure = item.optString("strMeasure$j").orEmpty()
                        if (measure.isNotEmpty()) {
                            measures.add(measure)
                        }
                    }

                    // Combine ingredients with measures (ingredient first)
                    val ingredientsWithMeasures = combineIngredientAndMeasure(ingredients, measures)

                    val description =
                        buildString {
                            append("Category: ${category.ifEmpty { categoryId }}")
                            if (alcoholic.isNotEmpty()) append(" • Type: $alcoholic")
                            if (glass.isNotEmpty()) append(" • Glass: $glass")
                            if (tags.isNotEmpty()) append(" • Tags: $tags")
                        }

                    result.add(
                        Cocktail(
                            id = id,
                            name = name,
                            description = description,
                            imageUrl = thumb.ifEmpty { Constants.PLACEHOLDER_IMAGE_URL },
                            ingredients = ingredientsWithMeasures.ifEmpty { listOf(Constants.DEFAULT_INGREDIENTS) },
                            instructions = Constants.DEFAULT_INSTRUCTIONS,
                            category = category.ifEmpty { categoryId },
                        ),
                    )
                }
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }

    override fun getCocktailById(id: String): Cocktail? {
        try {
            val url = baseUrl + "lookup.php?i=$id"
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val item = drinksArray.optJSONObject(0) ?: return null
            val name = item.optString("strDrink").orEmpty()
            val thumb = item.optString("strDrinkThumb").orEmpty()
            val instructions = item.optString("strInstructions").orEmpty()
            val category = item.optString("strCategory").orEmpty()
            val alcoholic = item.optString("strAlcoholic").orEmpty()
            val glass = item.optString("strGlass").orEmpty()
            val tags = item.optString("strTags").orEmpty()

            if (name.isEmpty()) return null

            // Extract ingredients (strIngredient1 to strIngredient15)
            val ingredients = mutableListOf<String>()
            val measures = mutableListOf<String>()

            for (i in 1..15) {
                val ingredient = item.optString("strIngredient$i").orEmpty()
                val measure = item.optString("strMeasure$i").orEmpty()

                if (ingredient.isNotEmpty()) {
                    ingredients.add(ingredient)
                    measures.add(measure)
                }
            }

            // Combine ingredients with measures (ingredient first)
            val ingredientsWithMeasures = combineIngredientAndMeasure(ingredients, measures)

            val description =
                buildString {
                    if (category.isNotEmpty()) append("Category: $category")
                    if (alcoholic.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append("Type: $alcoholic")
                    }
                    if (glass.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append("Glass: $glass")
                    }
                    if (tags.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append("Tags: $tags")
                    }
                }

            val cocktail = Cocktail(
                id = id,
                name = name,
                description = description.ifEmpty { Constants.DEFAULT_DESCRIPTION },
                imageUrl = thumb.ifEmpty { Constants.PLACEHOLDER_IMAGE_URL },
                ingredients = ingredientsWithMeasures.ifEmpty { listOf(Constants.DEFAULT_INGREDIENTS) },
                instructions = instructions.ifEmpty { Constants.DEFAULT_INSTRUCTIONS },
                category = category.ifEmpty { Constants.DEFAULT_CATEGORY },
            )
            return cocktail
        } catch (e: Exception) {
            return null
        }
    }

    override fun searchCocktails(query: String): List<Cocktail> {
        try {
            val url = baseUrl + "search.php?s=" + query.trim()
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<Cocktail>()

            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val id = item?.optString("idDrink").orEmpty()
                val name = item?.optString("strDrink").orEmpty()
                val thumb = item?.optString("strDrinkThumb").orEmpty()
                val category = item?.optString("strCategory").orEmpty()

                if (id.isNotEmpty() && name.isNotEmpty()) {
                    // Get detailed cocktail information by ID
                    val detailedCocktail = getCocktailById(id)
                    if (detailedCocktail != null) {
                        result.add(detailedCocktail)
                    } else {
                        // Fallback if detailed info is not available
                        val description =
                            buildString {
                                append("Search result for $query")
                                if (category.isNotEmpty()) append(" • Category: $category")
                            }

                        result.add(
                            Cocktail(
                                id = id,
                                name = name,
                                description = description,
                                imageUrl = thumb.ifEmpty { Constants.PLACEHOLDER_IMAGE_URL },
                                ingredients = listOf(Constants.DEFAULT_INGREDIENTS),
                                instructions = Constants.DEFAULT_INSTRUCTIONS,
                                category = category.ifEmpty { Constants.DEFAULT_CATEGORY },
                            ),
                        )
                    }
                }
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }

    // New search methods implementation
    override fun searchCocktailsByName(query: String): List<Cocktail> {
        try {
            val url = baseUrl + "search.php?s=" + query.trim()
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<Cocktail>()

            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val id = item?.optString("idDrink").orEmpty()
                val name = item?.optString("strDrink").orEmpty()
                val thumb = item?.optString("strDrinkThumb").orEmpty()
                val category = item?.optString("strCategory").orEmpty()

                if (id.isNotEmpty() && name.isNotEmpty()) {
                    // Get detailed cocktail information by ID
                    val detailedCocktail = getCocktailById(id)
                    if (detailedCocktail != null) {
                        result.add(detailedCocktail)
                    } else {
                        // Fallback if detailed info is not available
                        val description =
                            buildString {
                                if (category.isNotEmpty()) append("Category: $category")
                            }

                        result.add(
                            Cocktail(
                                id = id,
                                name = name,
                                description = description.ifEmpty { Constants.DEFAULT_DESCRIPTION },
                                imageUrl = thumb.ifEmpty { Constants.PLACEHOLDER_IMAGE_URL },
                                ingredients = listOf(Constants.DEFAULT_INGREDIENTS),
                                instructions = Constants.DEFAULT_INSTRUCTIONS,
                                category = category.ifEmpty { Constants.DEFAULT_CATEGORY },
                            ),
                        )
                    }
                }
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }

    override fun searchCocktailsByFirstLetter(letter: String): List<Cocktail> {
        try {
            val url = baseUrl + "search.php?f=" + letter.uppercase().first()
            Log.d(TAG, "Searching cocktails with URL: $url")

            val json = fetchJson(url)
            Log.d(TAG, "Received JSON: ${json.take(200)}...")

            val jsonObject = JSONObject(json)
            val drinksArray = jsonObject.optJSONArray("drinks")

            if (drinksArray == null) {
                Log.d(TAG, "No drinks array found in response")
                return emptyList()
            }

            Log.d(TAG, "Found ${drinksArray.length()} cocktails")
            val result = mutableListOf<Cocktail>()

            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val id = item?.optString("idDrink").orEmpty()
                val name = item?.optString("strDrink").orEmpty()
                val thumb = item?.optString("strDrinkThumb").orEmpty()
                val category = item?.optString("strCategory").orEmpty()
                val alcoholic = item?.optString("strAlcoholic").orEmpty()
                val glass = item?.optString("strGlass").orEmpty()
                val instructions = item?.optString("strInstructions").orEmpty()

                println("DEBUG: Processing cocktail $i: $name (ID: $id)")

                if (id.isNotEmpty() && name.isNotEmpty()) {
                    // Extract ingredients and measures directly from the response
                    val ingredients = mutableListOf<String>()
                    val measures = mutableListOf<String>()

                    for (j in 1..15) {
                        val ingredient = item.optString("strIngredient$j").orEmpty()
                        val measure = item.optString("strMeasure$j").orEmpty()

                        if (ingredient.isNotEmpty()) {
                            ingredients.add(ingredient)
                            measures.add(measure)
                        }
                    }

                    // Combine ingredients with measures (ingredient first)
                    val ingredientsWithMeasures = combineIngredientAndMeasure(ingredients, measures)

                    val description =
                        buildString {
                            append("Cocktails starting with ${letter.uppercase()}")
                            if (category.isNotEmpty()) append(" • Category: $category")
                            if (alcoholic.isNotEmpty()) append(" • Type: $alcoholic")
                            if (glass.isNotEmpty()) append(" • Glass: $glass")
                        }

                    result.add(
                        Cocktail(
                            id = id,
                            name = name,
                            description = description,
                            imageUrl = thumb.ifEmpty { Constants.PLACEHOLDER_IMAGE_URL },
                            ingredients = ingredientsWithMeasures.ifEmpty { listOf(Constants.DEFAULT_INGREDIENTS) },
                            instructions = instructions.ifEmpty { Constants.DEFAULT_INSTRUCTIONS },
                            category = category.ifEmpty { Constants.DEFAULT_CATEGORY },
                        ),
                    )
                    println("DEBUG: Added cocktail: $name with ${ingredientsWithMeasures.size} ingredients")
                }
            }

            println("DEBUG: Returning ${result.size} cocktails")

            // If no results from API, return fallback data
            if (result.isEmpty()) {
                println("DEBUG: No results from API, returning fallback data")
                return getFallbackCocktailsForLetter(letter)
            }

            return result
        } catch (e: Exception) {
            println("DEBUG: Error in searchCocktailsByFirstLetter: ${e.message}")
            e.printStackTrace()
            return getFallbackCocktailsForLetter(letter)
        }
    }

    override fun searchCocktailsByIngredient(ingredient: String): List<Cocktail> {
        try {
            // For ingredient search, we need to use filter.php?i=ingredient
            val filterUrl = baseUrl + "filter.php?i=" + ingredient.trim()
            val filterJson = fetchJson(filterUrl)
            val drinksArray = JSONObject(filterJson).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<Cocktail>()

            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val id = item?.optString("idDrink").orEmpty()
                val name = item?.optString("strDrink").orEmpty()
                val thumb = item?.optString("strDrinkThumb").orEmpty()
                val category = item?.optString("strCategory").orEmpty()

                if (id.isNotEmpty() && name.isNotEmpty()) {
                    // Get detailed cocktail information by ID
                    val detailedCocktail = getCocktailById(id)
                    if (detailedCocktail != null) {
                        result.add(detailedCocktail)
                    } else {
                        // Fallback if detailed info is not available
                        result.add(
                            Cocktail(
                                id = id,
                                name = name,
                                description = "Cocktails containing $ingredient",
                                imageUrl = thumb.ifEmpty { Constants.PLACEHOLDER_IMAGE_URL },
                                ingredients = listOf(Constants.DEFAULT_INGREDIENTS),
                                instructions = Constants.DEFAULT_INSTRUCTIONS,
                                category = category.ifEmpty { Constants.DEFAULT_CATEGORY },
                            ),
                        )
                    }
                }
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }

    override fun filterCocktailsByAlcoholic(isAlcoholic: Boolean): List<Cocktail> {
        try {
            val filterType = if (isAlcoholic) "Alcoholic" else "Non_Alcoholic"
            val url = baseUrl + "filter.php?a=$filterType"
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<Cocktail>()

            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val id = item?.optString("idDrink").orEmpty()
                val name = item?.optString("strDrink").orEmpty()
                val thumb = item?.optString("strDrinkThumb").orEmpty()
                val category = item?.optString("strCategory").orEmpty()

                if (id.isNotEmpty() && name.isNotEmpty()) {
                    // Get detailed cocktail information by ID
                    val detailedCocktail = getCocktailById(id)
                    if (detailedCocktail != null) {
                        result.add(detailedCocktail)
                    } else {
                        // Fallback if detailed info is not available
                        val description =
                            buildString {
                                append("${if (isAlcoholic) "Alcoholic" else "Non-alcoholic"} cocktail")
                                if (category.isNotEmpty()) append(" • Category: $category")
                            }

                        result.add(
                            Cocktail(
                                id = id,
                                name = name,
                                description = description,
                                imageUrl = thumb.ifEmpty { Constants.PLACEHOLDER_IMAGE_URL },
                                ingredients = listOf(Constants.DEFAULT_INGREDIENTS),
                                instructions = Constants.DEFAULT_INSTRUCTIONS,
                                category = category.ifEmpty { Constants.DEFAULT_CATEGORY },
                            ),
                        )
                    }
                }
            }

            // If no results from API, return fallback data
            if (result.isEmpty()) {
                println("DEBUG: No alcoholic filter results from API, returning fallback data")
                return getFallbackAlcoholicCocktails(isAlcoholic)
            }

            return result
        } catch (e: Exception) {
            println("DEBUG: Error in filterCocktailsByAlcoholic: ${e.message}")
            e.printStackTrace()
            return getFallbackAlcoholicCocktails(isAlcoholic)
        }
    }

    override fun filterCocktailsByCategory(category: String): List<Cocktail> {
        try {
            // Treat categoryId as the category name since API uses names
            val encoded = category.replace(' ', '_')
            val url = baseUrl + "filter.php?c=$encoded"
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<Cocktail>()

            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val id = item?.optString("idDrink").orEmpty()
                val name = item?.optString("strDrink").orEmpty()
                val thumb = item?.optString("strDrinkThumb").orEmpty()
                val categoryName = item?.optString("strCategory").orEmpty()
                val alcoholic = item?.optString("strAlcoholic").orEmpty()
                val glass = item?.optString("strGlass").orEmpty()
                val tags = item?.optString("strTags").orEmpty()

                if (id.isNotEmpty() && name.isNotEmpty()) {
                    // Extract ingredients (strIngredient1 to strIngredient15)
                    val ingredients = mutableListOf<String>()
                    for (j in 1..15) {
                        val ingredient = item.optString("strIngredient$j").orEmpty()
                        if (ingredient.isNotEmpty()) {
                            ingredients.add(ingredient)
                        }
                    }

                    // Extract measures (strMeasure1 to strMeasure15)
                    val measures = mutableListOf<String>()
                    for (j in 1..15) {
                        val measure = item.optString("strMeasure$j").orEmpty()
                        if (measure.isNotEmpty()) {
                            measures.add(measure)
                        }
                    }

                    // Combine ingredients with measures (ingredient first)
                    val ingredientsWithMeasures = combineIngredientAndMeasure(ingredients, measures)

                    val description =
                        buildString {
                            append("Category: ${categoryName.ifEmpty { category }}")
                            if (alcoholic.isNotEmpty()) append(" • Type: $alcoholic")
                            if (glass.isNotEmpty()) append(" • Glass: $glass")
                            if (tags.isNotEmpty()) append(" • Tags: $tags")
                        }

                    result.add(
                        Cocktail(
                            id = id,
                            name = name,
                            description = description,
                            imageUrl = thumb.ifEmpty { Constants.PLACEHOLDER_IMAGE_URL },
                            ingredients = ingredientsWithMeasures.ifEmpty { listOf(Constants.DEFAULT_INGREDIENTS) },
                            instructions = Constants.DEFAULT_INSTRUCTIONS,
                            category = categoryName.ifEmpty { category },
                        ),
                    )
                }
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }

    override fun getAlcoholicFilters(): List<String> {
        try {
            val url = baseUrl + "list.php?a=list"
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<String>()
            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val filter = item?.optString("strAlcoholic").orEmpty()
                if (filter.isNotEmpty()) {
                    result.add(filter)
                }
            }
            return result
        } catch (e: Exception) {
            return listOf("Alcoholic", "Non alcoholic", "Optional alcohol")
        }
    }

    override fun getGlassTypes(): List<String> {
        try {
            val url = baseUrl + "list.php?g=list"
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<String>()
            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val glass = item?.optString("strGlass").orEmpty()
                if (glass.isNotEmpty()) {
                    result.add(glass)
                }
            }
            return result
        } catch (e: Exception) {
            return listOf("Cocktail glass", "Champagne flute", "Wine glass", "Highball glass")
        }
    }

    override fun getIngredientsList(): List<String> {
        try {
            val url = baseUrl + "list.php?i=list"
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<String>()
            for (i in 0 until drinksArray.length()) {
                val item = drinksArray.optJSONObject(i)
                val ingredient = item?.optString("strIngredient1").orEmpty()
                if (ingredient.isNotEmpty()) {
                    result.add(ingredient)
                }
            }
            return result.sorted()
        } catch (e: Exception) {
            return listOf(
                "Vodka",
                "Gin",
                "Rum",
                "Tequila",
                "Whiskey",
                "Brandy",
                "Triple sec",
                "Lime juice",
                "Lemon juice",
                "Orange juice",
                "Cranberry juice",
                "Sugar",
                "Grenadine",
                "Bitters",
                "Champagne",
                "Coffee",
                "Milk",
            )
        }
    }

    override fun addFavourite(cocktailId: String) {
        val currentUser = firebase.getCurrentUser()
        if (currentUser == null) {
            Log.w(TAG, "addFavourite: no current user")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val favouritesRef = firestore.collection("favourite")

        favouritesRef
            .whereEqualTo("userID", currentUser.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // Create new favourites doc for this user
                    val data = hashMapOf(
                        "userID" to currentUser.uid,
                        "cocktails" to listOf(cocktailId),
                    )
                    favouritesRef
                        .add(data)
                        .addOnSuccessListener { Log.d(TAG, "addFavourite: created favourites doc and added $cocktailId") }
                        .addOnFailureListener { e -> Log.e(TAG, "addFavourite: failed to create doc", e) }
                } else {
                    // Update existing doc
                    val doc = querySnapshot.documents.first()
                    val currentList = (doc.get("cocktails") as? List<String>).orEmpty()
                    if (!currentList.contains(cocktailId)) {
                        val updated = currentList + cocktailId
                        favouritesRef
                            .document(doc.id)
                            .update("cocktails", updated)
                            .addOnSuccessListener { Log.d(TAG, "addFavourite: added $cocktailId to existing list") }
                            .addOnFailureListener { e -> Log.e(TAG, "addFavourite: failed to update list", e) }
                    } else {
                        Log.d(TAG, "addFavourite: cocktail already in favourites")
                    }
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "addFavourite: query failed", e) }
    }

    override fun removeFavourite(cocktailId: String) {
        val currentUser = firebase.getCurrentUser()
        if (currentUser == null) {
            Log.w(TAG, "removeFavourite: no current user")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val favouritesRef = firestore.collection("favourite")

        favouritesRef
            .whereEqualTo("userID", currentUser.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d(TAG, "removeFavourite: no favourites doc for user")
                } else {
                    val doc = querySnapshot.documents.first()
                    val currentList = (doc.get("cocktails") as? List<String>).orEmpty()
                    if (currentList.contains(cocktailId)) {
                        val updated = currentList.filter { it != cocktailId }
                        favouritesRef
                            .document(doc.id)
                            .update("cocktails", updated)
                            .addOnSuccessListener { Log.d(TAG, "removeFavourite: removed $cocktailId from favourites") }
                            .addOnFailureListener { e -> Log.e(TAG, "removeFavourite: failed to update list", e) }
                    } else {
                        Log.d(TAG, "removeFavourite: cocktail not in favourites")
                    }
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "removeFavourite: query failed", e) }
    }

    override fun getFavouriteCocktails(callback: (Result<List<Cocktail>>) -> Unit) {
        firebase.getCurrentUserFavouriteCocktails { result ->
            if (result.isFailure) {
                callback(Result.failure(result.exceptionOrNull() ?: Exception("Unknown error")))
                return@getCurrentUserFavouriteCocktails
            }

            val ids = result.getOrNull().orEmpty()
            if (ids.isEmpty()) {
                callback(Result.success(emptyList()))
                return@getCurrentUserFavouriteCocktails
            }

            // Fetch cocktail details off the main thread
            java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                val cocktails = mutableListOf<Cocktail>()
                for (id in ids) {
                    try {
                        val cocktail = getCocktailById(id)
                        if (cocktail != null) cocktails.add(cocktail)
                    } catch (e: Exception) {
                        Log.w(TAG, "getFavouriteCocktails: failed to fetch cocktail $id", e)
                    }
                }
                callback(Result.success(cocktails))
            }
        }
    }

    override fun toggleFavorite(cocktailId: String): Boolean = true

    override fun getFavoriteCocktails(): List<Cocktail> {
        try {
            // Use search by first letter 'F' to get some favorite cocktails
            val url = baseUrl + "search.php?f=F"
            val json = fetchJson(url)
            val drinksArray = JSONObject(json).optJSONArray("drinks") ?: JSONArray()
            val result = mutableListOf<Cocktail>()

            // Take first 4 cocktails as favorites
            val limit = minOf(4, drinksArray.length())

            for (i in 0 until limit) {
                val item = drinksArray.optJSONObject(i)
                val id = item?.optString("idDrink").orEmpty()
                val name = item?.optString("strDrink").orEmpty()
                val thumb = item?.optString("strDrinkThumb").orEmpty()
                val category = item?.optString("strCategory").orEmpty()
                val alcoholic = item?.optString("strAlcoholic").orEmpty()
                val glass = item?.optString("strGlass").orEmpty()
                val tags = item?.optString("strTags").orEmpty()

                if (id.isNotEmpty() && name.isNotEmpty()) {
                    // Extract ingredients (strIngredient1 to strIngredient15)
                    val ingredients = mutableListOf<String>()
                    val measures = mutableListOf<String>()

                    for (j in 1..15) {
                        val ingredient = item.optString("strIngredient$j").orEmpty()
                        val measure = item.optString("strMeasure$j").orEmpty()

                        if (ingredient.isNotEmpty()) {
                            ingredients.add(ingredient)
                            measures.add(measure)
                        }
                    }

                    // Combine ingredients with measures
                    val ingredientsWithMeasures =
                        if (ingredients.size == measures.size) {
                            ingredients.mapIndexed { index, ingredient ->
                                "${measures[index]} $ingredient".trim()
                            }
                        } else {
                            ingredients
                        }

                    val description =
                        buildString {
                            append("Favorite cocktail")
                            if (category.isNotEmpty()) append(" • Category: $category")
                            if (alcoholic.isNotEmpty()) append(" • Type: $alcoholic")
                            if (glass.isNotEmpty()) append(" • Glass: $glass")
                            if (tags.isNotEmpty()) append(" • Tags: $tags")
                        }

                    result.add(
                        Cocktail(
                            id = id,
                            name = name,
                            description = description,
                            imageUrl = thumb.ifEmpty { Constants.PLACEHOLDER_IMAGE_URL },
                            ingredients = ingredientsWithMeasures.ifEmpty { listOf(Constants.DEFAULT_INGREDIENTS) },
                            instructions = Constants.DEFAULT_INSTRUCTIONS,
                            category = category.ifEmpty { Constants.DEFAULT_CATEGORY },
                        ),
                    )
                }
            }
            return result
        } catch (e: Exception) {
            // Fallback to mock data if API fails
            return listOf(
                Cocktail(
                    id = "1",
                    name = "French 75",
                    description = "Classic champagne cocktail • Category: Cocktail • Type: Alcoholic • Glass: Champagne flute",
                    imageUrl = "https://www.thecocktaildb.com/images/media/drink/4qxyty1434360738.jpg",
                    ingredients = listOf("1 oz Gin", "1/2 oz Lemon juice", "1/2 oz Sugar syrup", "4 oz Champagne"),
                    instructions = "Combine gin, sugar, and lemon juice in a cocktail shaker...",
                    category = "Cocktail",
                ),
                Cocktail(
                    id = "2",
                    name = "Frozen Daiquiri",
                    description = "Frozen rum cocktail • Category: Cocktail • Type: Alcoholic • Glass: Cocktail glass",
                    imageUrl = "https://www.thecocktaildb.com/images/media/drink/7oyrj91504884412.jpg",
                    ingredients = listOf("1 1/2 oz Light rum", "1 oz Lime juice", "1/2 oz Triple sec", "1 tsp Sugar"),
                    instructions = "Pour all ingredients into an electric blender...",
                    category = "Cocktail",
                ),
            )
        }
    }

    private fun getFallbackCocktailsForLetter(letter: String): List<Cocktail> {
        val fallbackCocktails =
            mapOf(
                "A" to
                    listOf(
                        Cocktail(
                            id = "11007",
                            name = "Apple Martini",
                            description = "Classic cocktail starting with A • Category: Cocktail • Type: Alcoholic • Glass: Cocktail glass",
                            imageUrl = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg",
                            ingredients = listOf("1 1/2 oz Vodka", "1/2 oz Apple schnapps", "1 oz Apple juice", "1/2 oz Lemon juice"),
                            instructions = "Shake all ingredients with ice and strain into a chilled cocktail glass.",
                            category = "Cocktail",
                        ),
                        Cocktail(
                            id = "11008",
                            name = "Amaretto Sour",
                            description = "Sweet and sour cocktail • Category: Cocktail • Type: Alcoholic • Glass: Old-fashioned glass",
                            imageUrl = "https://www.thecocktaildb.com/images/media/drink/voxwvt1464176330.jpg",
                            ingredients = listOf("1 1/2 oz Amaretto", "1 oz Lemon juice", "1/2 oz Simple syrup", "1 Egg white"),
                            instructions = "Shake all ingredients with ice and strain into a chilled glass.",
                            category = "Cocktail",
                        ),
                    ),
                "B" to
                    listOf(
                        Cocktail(
                            id = "11009",
                            name = "Blue Lagoon",
                            description = "Refreshing blue cocktail • Category: Cocktail • Type: Alcoholic • Glass: Highball glass",
                            imageUrl = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg",
                            ingredients = listOf("1 oz Vodka", "1 oz Blue curacao", "4 oz Lemonade", "1 Maraschino cherry"),
                            instructions = "Pour vodka and curacao over ice in a highball glass. Fill with lemonade and stir gently.",
                            category = "Cocktail",
                        ),
                    ),
                "C" to
                    listOf(
                        Cocktail(
                            id = "11010",
                            name = "Cosmopolitan",
                            description = "Elegant vodka cocktail • Category: Cocktail • Type: Alcoholic • Glass: Cocktail glass",
                            imageUrl = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg",
                            ingredients = listOf("1 1/2 oz Vodka", "1/2 oz Triple sec", "1/2 oz Cranberry juice", "1/2 oz Lime juice"),
                            instructions = "Shake all ingredients with ice and strain into a chilled cocktail glass.",
                            category = "Cocktail",
                        ),
                    ),
                "D" to
                    listOf(
                        Cocktail(
                            id = "11011",
                            name = "Daiquiri",
                            description = "Classic rum cocktail • Category: Cocktail • Type: Alcoholic • Glass: Cocktail glass",
                            imageUrl = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg",
                            ingredients = listOf("2 oz White rum", "1 oz Lime juice", "1/2 oz Simple syrup"),
                            instructions = "Shake all ingredients with ice and strain into a chilled cocktail glass.",
                            category = "Cocktail",
                        ),
                    ),
            )

        return fallbackCocktails[letter.uppercase()] ?: listOf(
            Cocktail(
                id = "99999",
                name = "Sample Cocktail",
                description = "Sample cocktail starting with $letter • Category: Cocktail • Type: Alcoholic",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg",
                ingredients = listOf("1 oz Sample spirit", "1/2 oz Sample mixer", "1/2 oz Sample juice"),
                instructions = "Mix all ingredients with ice and serve.",
                category = "Cocktail",
            ),
        )
    }

    private fun getFallbackAlcoholicCocktails(isAlcoholic: Boolean): List<Cocktail> =
        if (isAlcoholic) {
            listOf(
                Cocktail(
                    id = "11012",
                    name = "Margarita",
                    description = "Classic tequila cocktail • Category: Ordinary Drink • Type: Alcoholic • Glass: Cocktail glass",
                    imageUrl = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg",
                    ingredients = listOf("1 1/2 oz Tequila", "1/2 oz Triple sec", "1 oz Lime juice", "Salt"),
                    instructions = "Rub the rim of the glass with the lime slice to make the salt stick to it. Shake the other ingredients with ice, then carefully pour into the glass.",
                    category = "Ordinary Drink",
                ),
                Cocktail(
                    id = "11013",
                    name = "Mojito",
                    description = "Refreshing mint cocktail • Category: Cocktail • Type: Alcoholic • Glass: Highball glass",
                    imageUrl = "https://www.thecocktaildb.com/images/media/drink/metwgh1606770327.jpg",
                    ingredients = listOf("2-3 oz White rum", "2 tbsp Fresh lime juice", "2-4 mint sprigs", "2 tsp Sugar"),
                    instructions = "Muddle mint leaves with sugar and lime juice. Add rum and fill with ice. Top with soda water and garnish with mint sprig.",
                    category = "Cocktail",
                ),
                Cocktail(
                    id = "11014",
                    name = "Martini",
                    description = "Elegant gin cocktail • Category: Cocktail • Type: Alcoholic • Glass: Cocktail glass",
                    imageUrl = "https://www.thecocktaildb.com/images/media/drink/71t8581504353095.jpg",
                    ingredients = listOf("1 2/3 oz Gin", "1/3 oz Dry Vermouth", "1 Olive"),
                    instructions = "Stir all ingredients with ice and strain into a chilled cocktail glass. Garnish with olive.",
                    category = "Cocktail",
                ),
            )
        } else {
            listOf(
                Cocktail(
                    id = "11015",
                    name = "Virgin Mojito",
                    description = "Non-alcoholic mint cocktail • Category: Cocktail • Type: Non-alcoholic • Glass: Highball glass",
                    imageUrl = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg",
                    ingredients = listOf("2 tbsp Fresh lime juice", "2-4 mint sprigs", "2 tsp Sugar", "Soda water"),
                    instructions = "Muddle mint leaves with sugar and lime juice. Fill with ice and top with soda water. Garnish with mint sprig.",
                    category = "Cocktail",
                ),
                Cocktail(
                    id = "11016",
                    name = "Shirley Temple",
                    description = "Classic non-alcoholic cocktail • Category: Cocktail • Type: Non-alcoholic • Glass: Highball glass",
                    imageUrl = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg",
                    ingredients = listOf("1 oz Grenadine", "6 oz Ginger ale", "1 Maraschino cherry"),
                    instructions = "Fill a highball glass with ice. Add grenadine and ginger ale. Garnish with cherry.",
                    category = "Cocktail",
                ),
            )
        }

    private fun fetchJson(urlString: String): String {
        val url = URL(urlString)
        val connection =
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                doInput = true
                // Add headers to avoid some API restrictions
                setRequestProperty("User-Agent", "CocktailDB-Android-App/1.0")
                setRequestProperty("Accept", "application/json")
            }
        try {
            val responseCode = connection.responseCode
            val stream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
            val reader = BufferedReader(InputStreamReader(stream))
            val builder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                builder.append(line)
            }
            reader.close()
            if (responseCode !in 200..299) {
                throw RuntimeException("HTTP $responseCode: $builder")
            }
            return builder.toString()
        } catch (e: Exception) {
            // Log the error for debugging
            println("DEBUG: Error fetching JSON from $urlString: ${e.message}")
            throw e
        } finally {
            connection.disconnect()
        }
    }
}
