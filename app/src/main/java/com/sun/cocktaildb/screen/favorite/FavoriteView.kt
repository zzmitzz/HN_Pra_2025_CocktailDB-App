package com.sun.cocktaildb.screen.favorite

import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.utils.base.BaseView

interface FavoriteView : BaseView {
    fun showFavorites(items: List<Cocktail>)
}