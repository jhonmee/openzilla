package com.openzilla.app.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap

/**
 * Single entry point to all persisted data. Every write is wrapped in a [Result] so a
 * ViewModel can show the user a clear error instead of the app silently losing data or
 * crashing mid-write.
 */
class HabitRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val habitDao = db.habitDao()
    private val reasonDao = db.reasonDao()
    private val historyDao = db.historyDao()

    /**
     * Last value seen for each habit. Reading a row from Room takes a few milliseconds, but
     * those milliseconds are a visible blank frame when opening a habit straight from the
     * list. Since the list was already observing the data, the detail screen can start from
     * what is known instead of from nothing.
     */
    private val habitCache = ConcurrentHashMap<Long, HabitEntity>()

    fun observeHabits(): Flow<List<HabitEntity>> = habitDao.observeAll()
        .onEach { habits -> habits.forEach { habitCache[it.id] = it } }

    fun observeHabit(id: Long): Flow<HabitEntity?> = habitDao.observeById(id)
        .onEach { habit -> habit?.let { habitCache[it.id] = it } }

    /** Whatever is already known about a habit, or null if it has never been observed. */
    fun cachedHabit(id: Long): HabitEntity? = habitCache[id]
    fun observeReasons(habitId: Long): Flow<List<ReasonEntity>> = reasonDao.observeForHabit(habitId)
    fun observeHistory(habitId: Long): Flow<List<HistoryEntity>> = historyDao.observeForHabit(habitId)
    fun observeAllHistory(): Flow<List<HistoryEntity>> = historyDao.observeAll()

    /** New habits go to the end of the list; the user can drag them anywhere afterwards. */
    suspend fun addHabit(habit: HabitEntity): Result<Long> = runCatching {
        habitDao.insert(habit.copy(sortOrder = habitDao.maxSortOrder() + 1))
    }

    /**
     * Persists a new manual order. [orderedIds] is the full list as the user left it on
     * screen; positions are rewritten from scratch inside one transaction, so the list can
     * never end up half-reordered if something fails midway.
     */
    suspend fun saveHabitOrder(orderedIds: List<Long>): Result<Unit> = runCatching {
        db.withTransaction {
            orderedIds.forEachIndexed { index, id -> habitDao.updateSortOrder(id, index) }
        }
    }

    suspend fun updateHabit(habit: HabitEntity): Result<Unit> = runCatching { habitDao.update(habit) }

    suspend fun deleteHabit(habit: HabitEntity): Result<Unit> = runCatching { habitDao.delete(habit) }

    /** Records the finished streak in history, then starts a fresh one — atomically. */
    suspend fun resetHabit(habit: HabitEntity, resetAt: Long, note: String? = null): Result<Unit> = runCatching {
        db.withTransaction {
            historyDao.insert(HistoryEntity(habitId = habit.id, streakStart = habit.startedAt, streakEnd = resetAt, note = note))
            habitDao.update(habit.copy(startedAt = resetAt))
        }
    }

    suspend fun addReason(habitId: Long, text: String): Result<Long> = runCatching {
        reasonDao.insert(ReasonEntity(habitId = habitId, text = text))
    }

    suspend fun deleteReason(reason: ReasonEntity): Result<Unit> = runCatching { reasonDao.delete(reason) }

    suspend fun longestPastStreakMillis(habitId: Long): Long = historyDao.longestPastStreakMillis(habitId) ?: 0L

    suspend fun relapseCount(habitId: Long): Int = historyDao.countForHabit(habitId)

    suspend fun getAllForExport(): ExportPayload = ExportPayload(
        habits = habitDao.getAllOnce(),
        reasons = reasonDao.getAllOnce(),
        history = historyDao.getAllOnce()
    )

    /** Replaces everything in one transaction: either the whole import lands, or none of it does. */
    suspend fun replaceAllWithImport(payload: ExportPayload): Result<Unit> = runCatching {
        db.withTransaction {
            historyDao.deleteAllOnce()
            reasonDao.deleteAllOnce()
            habitDao.deleteAll()
            payload.habits.forEach { habitDao.insert(it) }
            payload.reasons.forEach { reasonDao.insert(it) }
            payload.history.forEach { historyDao.insert(it) }
        }
    }

    /** Irreversible. Callers must have already gotten explicit, unambiguous user confirmation. */
    suspend fun deleteEverything(): Result<Unit> = runCatching {
        db.withTransaction {
            historyDao.deleteAllOnce()
            reasonDao.deleteAllOnce()
            habitDao.deleteAll()
        }
    }
}

data class ExportPayload(
    val habits: List<HabitEntity>,
    val reasons: List<ReasonEntity>,
    val history: List<HistoryEntity>
)
