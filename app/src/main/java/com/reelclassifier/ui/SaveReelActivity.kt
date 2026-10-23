package com.reelclassifier.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.reelclassifier.data.AppDatabase
import com.reelclassifier.data.Category
import com.reelclassifier.data.Reel
import com.reelclassifier.databinding.ActivitySaveReelBinding
import com.reelclassifier.util.ThumbnailHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SaveReelActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySaveReelBinding
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaveReelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = extractInstagramUrl(intent?.getStringExtra(Intent.EXTRA_TEXT) ?: "")
        if (url == null) {
            Toast.makeText(this, "No Instagram link found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvUrl.text = url.substringAfter("instagram.com/").take(50)

        val db = AppDatabase.get(this)
        db.categoryDao().getAll().observe(this) { categories ->
            binding.chipGroup.removeAllViews()
            categories.forEach { category ->
                val chip = Chip(this).apply {
                    text = category.name
                    isCheckable = true
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) selectedCategory = category.name
                        else if (selectedCategory == category.name) selectedCategory = null
                    }
                }
                binding.chipGroup.addView(chip)
            }
        }

        binding.btnSave.setOnClickListener {
            // New category field takes priority only if filled; else use chip selection
            val newCat = binding.etNewCategory.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val cat = newCat ?: selectedCategory
            if (cat == null) {
                Toast.makeText(this, "Pick or type a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val noteText = binding.etNote.text?.toString()?.trim() ?: ""
            val appContext = applicationContext
            lifecycleScope.launch(Dispatchers.IO) {
                if (newCat != null) db.categoryDao().insert(Category(cat))
                db.reelDao().insert(Reel(url = url, category = cat, note = noteText))

                // Capture thumbnail in background
                ThumbnailHelper.capture(appContext, url) { path ->
                    if (path != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val saved = db.reelDao().getReelsWithoutThumbnails().find { it.url == url }
                            if (saved != null) {
                                db.reelDao().updateThumbnail(saved.id, path)
                            }
                        }
                    }
                }

                runOnUiThread {
                    Toast.makeText(this@SaveReelActivity, "Saved to \"$cat\"", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        binding.btnCancel.setOnClickListener { finish() }
    }

    // Extract the first Instagram reel/post URL from shared text (may contain extra copy)
    private fun extractInstagramUrl(text: String): String? =
        Regex("https://www\\.instagram\\.com/(?:reel|p|tv)/[A-Za-z0-9_-]+/?")
            .find(text)?.value
}
