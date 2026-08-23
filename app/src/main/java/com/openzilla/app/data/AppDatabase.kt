package com.openzilla.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun fromCostType(value: HabitCostType): String = value.name

    @TypeConverter
    fun toCostType(value: String): HabitCostType =
        runCatching { HabitCostType.valueOf(value) }.getOrDefault(HabitCostType.EVENT)
}

/**
 * Adds the manual ordering column used by the home list.
 *
 * It is a real migration, never a destructive one: existing rows keep every value they had
 * and simply receive a starting position derived from the order they were already displayed
 * in (oldest first), so the list looks identical right after updating.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            UPDATE habits SET sortOrder = (
                SELECT COUNT(*) FROM habits AS earlier
                WHERE earlier.createdAt < habits.createdAt
                   OR (earlier.createdAt = habits.createdAt AND earlier.id < habits.id)
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [HabitEntity::class, ReasonEntity::class, HistoryEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun reasonDao(): ReasonDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "openzilla.db"
            )
                // Deliberately no fallbackToDestructiveMigration(): if a future schema change
                // ever ships without its migration, the app must fail loudly rather than
                // silently wipe the user's history.
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
