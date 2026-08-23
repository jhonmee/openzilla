package com.openzilla.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.openzilla.app.OpenZillaApp
import com.openzilla.app.R
import com.openzilla.app.util.TROPHIES
import kotlinx.coroutines.flow.first

/**
 * Once per run, checks whether any habit just crossed a trophy threshold since the last
 * check and, if so, posts one notification celebrating it. Pure read + a tiny in-memory
 * comparison — no writes, nothing kept running after [doWork] returns.
 */
class ProgressCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as OpenZillaApp
        val habits = app.repository.observeHabits().first()
        val now = System.currentTimeMillis()
        val windowMillis = 6L * 3_600_000L // matches the periodic check interval

        habits.forEach { habit ->
            val elapsed = now - habit.startedAt
            val justCrossed = TROPHIES.firstOrNull { elapsed >= it.durationMillis && elapsed - windowMillis < it.durationMillis }
            if (justCrossed != null) {
                notify(
                    applicationContext,
                    applicationContext.getString(R.string.notif_progress_title),
                    applicationContext.getString(R.string.notif_progress_body, habit.name, justCrossed.label)
                )
            }
        }
        return Result.success()
    }
}
