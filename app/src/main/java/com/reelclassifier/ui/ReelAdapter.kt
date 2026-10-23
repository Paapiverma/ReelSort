package com.reelclassifier.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reelclassifier.R
import com.reelclassifier.data.Reel
import com.reelclassifier.databinding.ItemReelBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReelAdapter(
    private val onOpen: (Reel) -> Unit,
    private val onDelete: (Reel) -> Unit
) : ListAdapter<Reel, ReelAdapter.VH>(DIFF) {

    private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    inner class VH(private val b: ItemReelBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(reel: Reel) {
            val shortId = reel.url.substringAfter("instagram.com/").trimEnd('/').take(35)

            // If note exists, it becomes the bold title!
            if (reel.note.isNotEmpty()) {
                b.tvTitle.text = reel.note
                b.tvSubtitle.text = shortId
                b.tvSubtitle.visibility = View.VISIBLE
            } else {
                b.tvTitle.text = shortId
                b.tvSubtitle.visibility = View.GONE
            }

            b.tvDate.text = dateFmt.format(Date(reel.savedAt))

            // Load thumbnail image if available
            val thumbFile = reel.thumbnailPath?.let { File(it) }
            if (thumbFile != null && thumbFile.exists() && thumbFile.length() > 1000) {
                try {
                    val bmp = BitmapFactory.decodeFile(thumbFile.absolutePath)
                    if (bmp != null) {
                        b.ivThumbnail.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        b.ivThumbnail.setImageBitmap(bmp)
                        b.ivThumbnail.imageTintList = null
                        b.ivThumbnail.background = null
                    } else {
                        setDefaultThumbnail()
                    }
                } catch (_: Exception) {
                    setDefaultThumbnail()
                }
            } else {
                setDefaultThumbnail()
            }

            b.root.setOnClickListener { onOpen(reel) }
            b.btnDelete.setOnClickListener { onDelete(reel) }
        }

        private fun setDefaultThumbnail() {
            b.ivThumbnail.scaleType = android.widget.ImageView.ScaleType.CENTER
            b.ivThumbnail.setImageResource(R.drawable.ic_bookmark)
            b.ivThumbnail.imageTintList = b.root.context.getColorStateList(R.color.icon_tint)
            b.ivThumbnail.setBackgroundResource(R.drawable.bg_icon_badge)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemReelBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Reel>() {
            override fun areItemsTheSame(a: Reel, b: Reel) = a.id == b.id
            override fun areContentsTheSame(a: Reel, b: Reel) = a == b
        }
    }
}
