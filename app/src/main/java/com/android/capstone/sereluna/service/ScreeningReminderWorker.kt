package com.android.capstone.sereluna.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.repository.NotificationRepository
import com.android.capstone.sereluna.data.repository.ScreeningRepository
import com.android.capstone.sereluna.ui.diary.ScreeningActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreeningReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val screeningRepository = ScreeningRepository()
    private val notificationRepository = NotificationRepository()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun doWork(): Result {
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        return try {
            val today = dateFormatter.format(Date())
            if (wasReminderSentToday(today)) return Result.success()

            val alreadyScreened = screeningRepository.hasTodayScreening()
            if (!alreadyScreened) {
                notificationRepository.addNotification(
                    title = applicationContext.getString(R.string.screening_reminder_title),
                    body = applicationContext.getString(R.string.screening_reminder_body),
                    type = "reminder"
                )
                showSystemNotification()
                markReminderSent(today)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showSystemNotification() {
        if (!canPostNotifications()) return

        val channelId = applicationContext.getString(R.string.screening_notification_channel_id)
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                applicationContext.getString(R.string.screening_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val screeningIntent = Intent(applicationContext, ScreeningActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE,
            screeningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(applicationContext.getString(R.string.screening_reminder_title))
            .setContentText(applicationContext.getString(R.string.screening_reminder_body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun wasReminderSentToday(today: String): Boolean {
        return prefs().getString(KEY_LAST_REMINDER_DATE, null) == today
    }

    private fun markReminderSent(today: String) {
        prefs().edit().putString(KEY_LAST_REMINDER_DATE, today).apply()
    }

    private fun prefs() =
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "screening_reminder"
        private const val KEY_LAST_REMINDER_DATE = "last_reminder_date"
        private const val REQUEST_CODE = 2101
        private const val NOTIFICATION_ID = 2101
    }
}
