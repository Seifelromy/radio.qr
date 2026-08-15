package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [StreamEntity::class, DiagnosticLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun streamDao(): StreamDao
    abstract fun diagnosticDao(): DiagnosticDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbBuilder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mizan_radio_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default streams in background thread
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            database.streamDao().insertStreams(getDefaultStreams())
                            database.diagnosticDao().insertLog(
                                DiagnosticLogEntity(
                                    eventType = "SUCCESS",
                                    message = "تم إنشاء قاعدة البيانات الرقمية وتأمين مسارات البث الثلاثة."
                                )
                            )
                        }
                    }
                })
                
                val instance = dbBuilder.build()
                INSTANCE = instance
                instance
            }
        }

        fun getDefaultStreams(): List<StreamEntity> {
            return listOf(
                StreamEntity(
                    id = 1,
                    url = "https://stream.radiojar.com/8s5u5tpdtwzuv",
                    name = "Official Primary Stream",
                    displayNameAr = "إذاعة القرآن الكريم من القاهرة (المصدر الرئيسي)",
                    isPreferred = true,
                    isHealthy = true,
                    rank = 1
                ),
                StreamEntity(
                    id = 2,
                    url = "http://live.mp3quran.net:9722",
                    name = "Mirror Backup Stream",
                    displayNameAr = "خادم شبكة MP3Quran (مسرى احتياطي)",
                    isPreferred = false,
                    isHealthy = true,
                    rank = 2
                ),
                StreamEntity(
                    id = 3,
                    url = "http://66.45.232.131:9994/;stream.mp3",
                    name = "Community Backup Stream",
                    displayNameAr = "المجرى المشترك الطارئ (خادم شعبي)",
                    isPreferred = false,
                    isHealthy = true,
                    rank = 3
                )
            )
        }
    }
}
