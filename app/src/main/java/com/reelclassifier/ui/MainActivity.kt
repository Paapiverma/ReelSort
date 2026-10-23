package com.reelclassifier.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.reelclassifier.data.AppDatabase
import com.reelclassifier.data.Category
import com.reelclassifier.databinding.ActivityMainBinding
import com.reelclassifier.service.OverlayService
import com.reelclassifier.util.AppUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val db = AppDatabase.get(this)
        val adapter = CategoryAdapter(
            onClick = { category ->
                startActivity(Intent(this, CategoryReelsActivity::class.java)
                    .putExtra("category", category.name))
            },
            onDelete = { category -> confirmDeleteCategory(db, category) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        db.categoryDao().getAll().observe(this) { adapter.submitList(it) }

        binding.fabAddCategory.setOnClickListener { showAddCategoryDialog(db) }

        AppUpdater.checkForUpdates(this)

        binding.btnToggleOverlay.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                toggleOverlay()
            } else {
                // Ask user to grant overlay permission
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.btnToggleOverlay.text =
            if (OverlayService.isRunning) "Disable Floating Button" else "Enable Floating Button"
    }

    private fun toggleOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        if (OverlayService.isRunning) {
            stopService(intent)
        } else {
            startForegroundService(intent)
        }
        // onResume will update the button label
    }

    private fun confirmDeleteCategory(db: AppDatabase, category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Delete \"${category.name}\"?")
            .setMessage("All saved reels in this category will also be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.reelDao().deleteByCategory(category.name)
                    db.categoryDao().delete(category)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddCategoryDialog(db: AppDatabase) {
        val input = EditText(this).apply {
            hint = "Category name"
            setTextColor(getColor(com.reelclassifier.R.color.text_primary))
            setHintTextColor(getColor(com.reelclassifier.R.color.text_muted))
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("New Category")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.categoryDao().insert(Category(name))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
