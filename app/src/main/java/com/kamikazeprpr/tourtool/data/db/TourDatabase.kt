package com.kamikazeprpr.tourtool.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Team::class, Member::class, Tag::class, MemberTag::class],
    version = 2,
    exportSchema = true,
)
abstract class TourDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun memberDao(): MemberDao
    abstract fun tagDao(): TagDao

    companion object {
        private const val NAME = "tour.db"

        /** v1 → v2：团里加了「结束日期」，老用户升级不能丢数据 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE team ADD COLUMN endDate TEXT")
            }
        }

        @Volatile
        private var instance: TourDatabase? = null

        fun get(context: Context): TourDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TourDatabase::class.java,
                    NAME,
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 预置几个高频标签，用户可以随时删改
                            val presets = listOf("孕妇", "高龄", "儿童", "免票", "半票", "退伍军人")
                            presets.forEachIndexed { i, label ->
                                db.execSQL(
                                    "INSERT INTO tag (label, colorIndex, sortOrder) VALUES (?, ?, ?)",
                                    arrayOf(label, i % 6, i),
                                )
                            }
                        }
                    })
                    .build()
                    .also { instance = it }
            }
    }
}
