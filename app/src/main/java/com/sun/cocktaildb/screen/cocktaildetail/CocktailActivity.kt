package com.sun.cocktaildb.screen.cocktaildetail

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.sun.cocktaildb.R
import com.sun.cocktaildb.data.model.Cocktail
import com.sun.cocktaildb.data.repository.impl.CocktailRepositoryImpl
import com.sun.cocktaildb.databinding.ActivityDetailBinding
import com.sun.cocktaildb.screen.cocktaildetail.adapter.IngredientAdapter
import com.sun.cocktaildb.utils.ImageLoader
import com.sun.cocktaildb.utils.base.BaseActivity
import com.sun.cocktaildb.utils.dialog.LoadingDialog
import com.sun.cocktaildb.utils.Constants

class CocktailActivity :
    BaseActivity(),
    CocktailContract.View {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var presenter: CocktailPresenter
    private lateinit var ingredientAdapter: IngredientAdapter

    private val loadingDialog by lazy {
        LoadingDialog(this)
    }

    companion object {
        private const val EXTRA_COCKTAIL_ID = "extra_cocktail_id"

        fun newIntent(
            context: Context,
            cocktailId: String,
        ): Intent =
            Intent(context, CocktailActivity::class.java).apply {
                putExtra(EXTRA_COCKTAIL_ID, cocktailId)
            }
    }

    override fun initView() {
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPresenter()
        setupRecyclerView()
        setupClickListeners()

        // Load cocktail details
        val cocktailId = intent.getStringExtra(EXTRA_COCKTAIL_ID)
        if (cocktailId != null) {
            presenter.loadCocktailDetail(cocktailId)
        } else {
            showError(getString(R.string.invalid_cocktail_id))
        }
    }

    private fun setupPresenter() {
        presenter = CocktailPresenter(CocktailRepositoryImpl())
        presenter.setView(this)
    }

    private fun setupRecyclerView() {
        ingredientAdapter = IngredientAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CocktailActivity)
            adapter = ingredientAdapter
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }

        // Top favorite button (in image overlay)
        binding.ivFavorite.setOnClickListener {
            // This will be updated when cocktail is loaded
        }

        // Top share button (in image overlay)
        binding.ivShare.setOnClickListener {
        }
    }

    override fun showCocktailDetail(cocktail: Cocktail) {
        binding.apply {
            // Set cocktail image
            if (cocktail.imageUrl.isNotEmpty() && cocktail.imageUrl != Constants.PLACEHOLDER_IMAGE_URL) {
                ImageLoader.loadImage(ivCocktail, cocktail.imageUrl, R.drawable.placeholder)
            } else {
                ivCocktail.setImageResource(R.drawable.placeholder)
            }

            // Set title
            tvTitle.text = cocktail.name

            // Set tags (category and alcoholic status)
            val tags = mutableListOf<String>()
            if (cocktail.category.isNotEmpty() && cocktail.category != Constants.DEFAULT_CATEGORY) {
                tags.add(cocktail.category)
            }

            // Determine alcoholic status from description or use default
            val isAlcoholic = !cocktail.description.contains("Non alcoholic", ignoreCase = true)
            tags.add(if (isAlcoholic) getString(R.string.alcoholic) else getString(R.string.non_alcoholic))

            // Update tags layout
            updateTagsLayout(tags)

            // Set name row
            tvCocktailName.text = cocktail.name

            // Set ingredients
            ingredientAdapter.updateIngredients(
                cocktail.ingredients.filter {
                    !it.contains("null", ignoreCase = true)
                },
            )

            // Set description
            tvDescription.text = cocktail.instructions.ifEmpty { getString(R.string.instructions_not_available) }

            // Setup favorite and share buttons
            setupActionButtons(cocktail)
        }
    }

    private fun updateTagsLayout(tags: List<String>) {
        binding.tagsLayout.removeAllViews()

        tags.forEach { tag ->
            val tagView = layoutInflater.inflate(R.layout.item_tag, binding.tagsLayout, false)
            val tagTextView = tagView.findViewById<android.widget.TextView>(R.id.tvTag)
            tagTextView.text = tag

            binding.tagsLayout.addView(tagView)

            // Add margin between tags
            if (tag != tags.last()) {
                val params = tagView.layoutParams as android.widget.LinearLayout.LayoutParams
                params.marginEnd = resources.getDimensionPixelSize(R.dimen.tag_margin)
                tagView.layoutParams = params
            }
        }
    }

    private fun setupActionButtons(cocktail: Cocktail) {
        // Top favorite button (in image overlay)
        binding.ivFavorite.setOnClickListener {
            presenter.toggleFavorite(cocktail)
        }

        // Top share button (in image overlay)
        binding.ivShare.setOnClickListener {
            presenter.shareCocktail(cocktail)
        }

        // Bottom favorite button
        binding.btnFavorite.setOnClickListener {
            presenter.toggleFavorite(cocktail)
        }

        // Bottom share button
        binding.btnShare.setOnClickListener {
            presenter.shareCocktail(cocktail)
        }
    }

    override fun showLoading() {
        loadingDialog.show()
    }

    override fun hideLoading() {
        loadingDialog.hide()
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun updateFavoriteButton(isFavorite: Boolean) {
        // Update top favorite button
        binding.ivFavorite.apply {
            if (isFavorite) {
                setImageResource(R.drawable.ic_favorite_filled_black_24dp)
            } else {
                setImageResource(R.drawable.ic_favorite_border_black_24dp)
            }
        }

        // Update bottom favorite button
        binding.btnFavorite.apply {
            if (isFavorite) {
                setText(R.string.remove_from_favorites)
                setBackgroundResource(R.drawable.button_orange)
            } else {
                setText(R.string.add_to_favorites)
                setBackgroundResource(R.drawable.rounded_box)
            }
        }
    }

    override fun showShareDialog(cocktail: Cocktail) {
        val shareIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
                putExtra(
                    Intent.EXTRA_TEXT,
                    cocktail.name,
                )
            }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }

    override fun onResume() {
        super.onResume()
        presenter.onStart()
    }

    override fun onPause() {
        super.onPause()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}
