package com.openzilla.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HabitDto(
    val id: Long,
    val name: String,
    val iconKey: String,
    val costType: String,
    val weeklyAmount: Double?,
    val startedAt: Long,
    val goalHours: Int,
    val createdAt: Long,
    // Con valor por defecto para que las copias hechas antes de que existiera el orden
    // manual se sigan importando sin tocar nada (y las nuevas se lean en versiones viejas).
    val sortOrder: Int = 0
)

@Serializable
data class ReasonDto(val id: Long, val habitId: Long, val text: String, val createdAt: Long)

@Serializable
data class HistoryDto(val id: Long, val habitId: Long, val streakStart: Long, val streakEnd: Long, val note: String?)

@Serializable
data class ExportFile(
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val habits: List<HabitDto>,
    val reasons: List<ReasonDto>,
    val history: List<HistoryDto>
)

/**
 * Turns the local database into a single self-contained JSON file the user chooses a
 * destination for (Storage Access Framework — OpenZilla never picks a cloud location and
 * never uploads anything). Import mirrors this in reverse and is fully local too.
 */
class ExportImportManager(private val context: Context, private val repository: HabitRepository) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val data = repository.getAllForExport()
            val file = ExportFile(
                exportedAt = System.currentTimeMillis(),
                habits = data.habits.map { HabitDto(it.id, it.name, it.iconKey, it.costType.name, it.weeklyAmount, it.startedAt, it.goalHours, it.createdAt, it.sortOrder) },
                reasons = data.reasons.map { ReasonDto(it.id, it.habitId, it.text, it.createdAt) },
                history = data.history.map { HistoryDto(it.id, it.habitId, it.streakStart, it.streakEnd, it.note) }
            )
            val text = json.encodeToString(file)
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
            } ?: error("No se pudo abrir el archivo destino")
        }
    }

    /** Parses fully into memory first; only if that succeeds do we touch the database. */
    suspend fun importFrom(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: error("No se pudo leer el archivo")
            val file = json.decodeFromString<ExportFile>(text)
            require(file.formatVersion <= 1) { "Este archivo es de una versión más nueva de OpenZilla" }

            val payload = ExportPayload(
                habits = file.habits.map {
                    HabitEntity(it.id, it.name, it.iconKey, runCatching { HabitCostType.valueOf(it.costType) }.getOrDefault(HabitCostType.EVENT), it.weeklyAmount, it.startedAt, it.goalHours, it.createdAt, it.sortOrder)
                },
                reasons = file.reasons.map { ReasonEntity(it.id, it.habitId, it.text, it.createdAt) },
                history = file.history.map { HistoryEntity(it.id, it.habitId, it.streakStart, it.streakEnd, it.note) }
            )
            repository.replaceAllWithImport(payload).getOrThrow()
            payload.habits.size
        }
    }
}
