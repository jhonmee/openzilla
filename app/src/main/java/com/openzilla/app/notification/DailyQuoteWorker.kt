package com.openzilla.app.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.openzilla.app.R
import com.openzilla.app.util.quoteOfTheDay

class DailyQuoteWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val quote = quoteOfTheDay()
        notify(applicationContext, applicationContext.getString(R.string.notif_daily_quote_title), "${quote.text} — ${quote.author}")
        return Result.success()
    }
}

/** Small shared helper so both workers post notifications the same safe way. */
internal fun notify(context: Context, title: String, body: String) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        return
    }
    val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
}
