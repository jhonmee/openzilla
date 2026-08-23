package com.openzilla.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.openzilla.app.R
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * All notifications are scheduled through WorkManager's periodic jobs — batched by the OS,
 * Doze-aware, and automatically stopped/cleaned up when cancelled. There is no foreground
 * service and nothing runs continuously in the background between firings.
 */
object NotificationScheduler {
    const val CHANNEL_ID = "openzilla_default"
    private const val QUOTE_WORK = "openzilla_daily_quote"
    private const val PROGRESS_WORK = "openzilla_progress_check"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            channel.description = context.getString(R.string.notif_channel_desc)
            manager?.createNotificationChannel(channel)
        }
    }

    fun setDailyQuoteEnabled(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<DailyQuoteWorker>(1, TimeUnit.DAYS).build()
            wm.enqueueUniquePeriodicWork(QUOTE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            wm.cancelUniqueWork(QUOTE_WORK)
        }
    }

    fun setProgressChecksEnabled(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<ProgressCheckWorker>(6, TimeUnit.HOURS).build()
            wm.enqueueUniquePeriodicWork(PROGRESS_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            wm.cancelUniqueWork(PROGRESS_WORK)
        }
    }
}
