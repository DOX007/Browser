package com.cookiejarapps.android.smartcookieweb.browser.shortcuts

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(
    entities = [ShortcutEntity::class],
    version = 2
)
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
                                ShortcutEntity(uid = 0, title = "Google", url = "https://www.google.com", add = false),
                                ShortcutEntity(uid = 0, title = "Wikipedia", url = "https://www.wikipedia.org", add = false),
                                ShortcutEntity(uid = 0, title = "YouTube", url = "https://www.youtube.com", add = false),
                                ShortcutEntity(uid = 0, title = "Reddit", url = "https://www.reddit.com", add = false),
                                ShortcutEntity(uid = 0, title = "GitHub", url = "https://www.github.com", add = false),
                                ShortcutEntity(uid = 0, title = "Stack Overflow", url = "https://stackoverflow.com", add = false),
                                ShortcutEntity(uid = 0, title = "Twitter", url = "https://twitter.com", add = false),
                                ShortcutEntity(uid = 0, title = "Facebook", url = "https://www.facebook.com", add = false),
                                ShortcutEntity(uid = 0, title = "Instagram", url = "https://www.instagram.com", add = false),
                                ShortcutEntity(uid = 0, title = "Amazon", url = "https://www.amazon.com", add = false)
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

