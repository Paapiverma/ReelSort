package com.reelclassifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
