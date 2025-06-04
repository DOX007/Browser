package com.cookiejarapps.android.smartcookieweb.browser.shortcuts

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(entities = [ShortcutEntity::class], version = 2)
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
                    "shortcuts_database2" // OBS: byt namn om du vill tvinga ny databas vid test!
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d("Shortcuts", "RoomDatabase onCreate() körs, försöker lägga in default-shortcuts!")
                            Executors.newSingleThreadExecutor().execute {
                                getDatabase(context).shortcutDao().insertAll(
                                    ShortcutEntity(uid = 0, title = "V8-Bibliotek", url = "https://www.v8biblioteken.se/sv/library-page/lycksele", add = false),
                                    ShortcutEntity(uid = 0, title = "Illustrerad Vetenskap", url = "https://illvet.se", add = false),
                                    ShortcutEntity(uid = 0, title = "Uppslagsverk", url = "https://ne.se", add = false),
                                    ShortcutEntity(uid = 0, title = "svenska.se", url = "https://svenska.se", add = false),
                                    ShortcutEntity(uid = 0, title = "Synonymer", url = "https://www.synonymer.se", add = false),
                                    ShortcutEntity(uid = 0, title = "Folkets-lexikon", url = "https://folkets-lexikon.csc.kth.se/folkets/folkets.en.html", add = false),
                                    ShortcutEntity(uid = 0, title = "Cambridge.org", url = "https://dictionary.cambridge.org", add = false),
                                    ShortcutEntity(uid = 0, title = "Oxford Dictionary", url = "https://www.oed.com/?tl=true", add = false),
                                    ShortcutEntity(uid = 0, title = "Kryssakuten", url = "https://www.kryssakuten.se/korsordshj%C3%A4lp", add = false),
                                    ShortcutEntity(uid = 0, title = "Korsords_hjalp", url = "https://tyda.se/korsords_hjalp", add = false)
                                )
                                Log.d("Shortcuts", "insertAll() körd för default-shortcuts!")
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
