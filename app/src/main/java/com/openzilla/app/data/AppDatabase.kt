package com.openzilla.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Room

class Converters {
    @TypeConverter
    fun fromCostType(value: HabitCostType): String = value.name

    @TypeConverter
    fun toCostType(value: String): HabitCostType =
        runCatching { HabitCostType.valueOf(value) }.getOrDefault(HabitCostType.EVENT)
}

@Database(
    entities = [HabitEntity::class, ReasonEntity::class, HistoryEntity::class],
    version = 1,
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
            ).build().also { instance = it }
        }
    }
}
