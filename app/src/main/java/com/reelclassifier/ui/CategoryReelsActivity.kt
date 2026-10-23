package com.reelclassifier.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.reelclassifier.data.AppDatabase
import com.reelclassifier.databinding.ActivityCategoryReelsBinding
import com.reelclassifier.util.ThumbnailHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryReelsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryReelsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryReelsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val category = intent.getStringExtra("category") ?: return finish()
        supportActionBar?.title = category
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val db = AppDatabase.get(this)
        val adapter = ReelAdapter(
            onOpen = { reel ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(reel.url)))
            },
            onDelete = { reel ->
                lifecycleScope.launch(Dispatchers.IO) { db.reelDao().delete(reel) }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        db.reelDao().getByCategory(category).observe(this) { reels ->
            adapter.submitList(reels)
            // Background load thumbnails for reels that don't have one yet
            reels.filter { it.thumbnailPath.isNullOrEmpty() }.forEach { reel ->
                ThumbnailHelper.capture(applicationContext, reel.url) { path ->
                    if (path != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            db.reelDao().updateThumbnail(reel.id, path)
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
