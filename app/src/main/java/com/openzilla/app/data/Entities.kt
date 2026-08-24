package com.openzilla.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** What kind of cost a habit represents — mirrors the wizard's "Dinero / Tiempo / Evento" step. */
enum class HabitCostType { MONEY, TIME, EVENT }

/**
 * A habit/commitment the user is trying to quit or reduce.
 * [startedAt] is the timestamp the current streak began (i.e. "last time" from the wizard,
 * or the last reset). It is the single source of truth for the live counters — nothing else
 * duplicates it, so there is nothing that can drift out of sync.
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val costType: HabitCostType,
    val weeklyAmount: Double?,
    val startedAt: Long,
    val goalHours: Int = 24,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Position in the home list. Lower comes first; the user sets it by dragging.
     *
     * The default is declared here as well as in the migration so the schema Room expects and
     * the one actually on disk match exactly, whether the database was created fresh or
     * migrated from version 1.
     */
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,
    /** Cuándo se regó la planta por última vez. 0 = nunca. */
    @ColumnInfo(defaultValue = "0")
    val lastWateredAt: Long = 0,
    /**
     * Tiempo de recuperación ya ganado a base de riegos, en milisegundos. Se pone a cero en
     * cada recaída: el riego acelera la recuperación en curso, no se acumula para la próxima.
     */
    @ColumnInfo(defaultValue = "0")
    val recoveryBonusMillis: Long = 0
)

/** A personal, free-text reason the user wrote for quitting a given habit. */
@Entity(
    tableName = "reasons",
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class ReasonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * One completed streak, recorded whenever the user resets a habit. Used to compute the
 * longest streak and simple history stats without ever mutating past records.
 */
@Entity(
    tableName = "history",
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val streakStart: Long,
    val streakEnd: Long,
    val note: String? = null
)
