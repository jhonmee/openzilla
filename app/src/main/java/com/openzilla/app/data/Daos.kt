package com.openzilla.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeById(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Query("DELETE FROM habits")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun count(): Int

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<HabitEntity>
}

@Dao
interface ReasonDao {
    @Query("SELECT * FROM reasons WHERE habitId = :habitId ORDER BY createdAt ASC")
    fun observeForHabit(habitId: Long): Flow<List<ReasonEntity>>

    @Insert
    suspend fun insert(reason: ReasonEntity): Long

    @Delete
    suspend fun delete(reason: ReasonEntity)

    @Query("SELECT * FROM reasons ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<ReasonEntity>

    @Query("DELETE FROM reasons")
    suspend fun deleteAllOnce()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE habitId = :habitId ORDER BY streakEnd DESC")
    fun observeForHabit(habitId: Long): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    @Query("SELECT COUNT(*) FROM history WHERE habitId = :habitId")
    suspend fun countForHabit(habitId: Long): Int

    @Query("SELECT MAX(streakEnd - streakStart) FROM history WHERE habitId = :habitId")
    suspend fun longestPastStreakMillis(habitId: Long): Long?

    @Query("SELECT * FROM history ORDER BY streakEnd ASC")
    suspend fun getAllOnce(): List<HistoryEntity>

    @Query("DELETE FROM history")
    suspend fun deleteAllOnce()
}
