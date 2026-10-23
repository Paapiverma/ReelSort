package com.reelclassifier.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reelclassifier.data.Category
import com.reelclassifier.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onClick: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) :
    ListAdapter<Category, CategoryAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(category: Category) {
            b.tvCategoryName.text = category.name
            b.root.setOnClickListener { onClick(category) }
            b.btnDeleteCategory.setOnClickListener { onDelete(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(a: Category, b: Category) = a.name == b.name
            override fun areContentsTheSame(a: Category, b: Category) = a == b
        }
    }
}
