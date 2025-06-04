package com.cookiejarapps.android.smartcookieweb.browser.shortcuts

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(entities = [ShortcutEntity::class], version = 2) // Ändra version om du gjort ändringar!
abstract class ShortcutDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile
        private var INSTANCE: ShortcutDatabase? = null

        fun getDatabase(context: Context): ShortcutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShortcutDatabase::class.java,
                    "shortcuts_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Executors.newSingleThreadExecutor().execute {
                            getDatabase(context).shortcutDao().insertAll(
                                ShortcutEntity(url = "https://www.google.com", add = false, title = "Google"),
                                ShortcutEntity(url = "https://www.wikipedia.org", add = false, title = "Wikipedia"),
                                ShortcutEntity(url = "https://www.youtube.com", add = false, title = "YouTube"),
                                ShortcutEntity(url = "https://www.reddit.com", add = false, title = "Reddit"),
                                ShortcutEntity(url = "https://www.github.com", add = false, title = "GitHub"),
                                ShortcutEntity(url = "https://stackoverflow.com", add = false, title = "Stack Overflow"),
                                ShortcutEntity(url = "https://twitter.com", add = false, title = "Twitter"),
                                ShortcutEntity(url = "https://www.facebook.com", add = false, title = "Facebook"),
                                ShortcutEntity(url = "https://www.instagram.com", add = false, title = "Instagram"),
                                ShortcutEntity(url = "https://www.amazon.com", add = false, title = "Amazon")
                            )
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

