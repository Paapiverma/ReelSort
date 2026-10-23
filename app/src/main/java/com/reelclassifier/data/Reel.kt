package com.reelclassifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reels")
data class Reel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val category: String,
    val note: String = "",
    val thumbnailPath: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)
