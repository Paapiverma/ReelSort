package com.reelclassifier.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Reel::class, Category::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reelDao(): ReelDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val DEFAULT_CATEGORIES = listOf(
            "AI & Machine Learning", "Mobile Dev", "Web Dev",
            "DevOps & Cloud", "Programming Tips", "System Design", "Open Source"
        )

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "reels.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    // Seed categories with raw SQL — no DAO needed, runs inside onCreate transaction
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val now = System.currentTimeMillis()
                        DEFAULT_CATEGORIES.forEachIndexed { i, name ->
                            val safe = name.replace("'", "''")
                            db.execSQL("INSERT OR IGNORE INTO categories(name, createdAt) VALUES('$safe', ${now + i})")
                        }
                    }
                })
                .build().also { INSTANCE = it }
        }

        // Adds the note column; existing reels get an empty string
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reels ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        // Adds the thumbnailPath column; existing reels get NULL
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reels ADD COLUMN thumbnailPath TEXT DEFAULT NULL")
            }
        }
    }
}
