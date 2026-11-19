package com.kamikazeprpr.tourtool.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Team::class, Member::class, Tag::class, MemberTag::class],
    version = 4,
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

        /** v3 → v4：让每个标签拿到互不重复的颜色下标（本来是 6 个来回用，10 个标签就撞色了） */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """UPDATE tag SET colorIndex = (
                           SELECT COUNT(*) FROM tag t2
                           WHERE t2.sortOrder < tag.sortOrder
                              OR (t2.sortOrder = tag.sortOrder AND t2.id < tag.id)
                       )"""
                )
            }
        }

        /** v2 → v3：预置几种常用的出行标签，颜色下标固定，界面按它取底色 */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val presets = listOf("老人", "儿童", "孕妇", "素食", "清真", "行动不便")
                presets.forEachIndexed { index, label ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO tag (label, colorIndex, sortOrder) VALUES (?, ?, ?)",
                        arrayOf<Any>(label, index, index),
                    )
                }
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
