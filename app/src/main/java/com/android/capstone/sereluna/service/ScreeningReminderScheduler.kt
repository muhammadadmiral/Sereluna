package com.android.capstone.sereluna.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ScreeningReminderScheduler {

    private const val WORK_NAME = "daily_screening_reminder"
    private const val REMINDER_HOUR = 20
    private const val REMINDER_MINUTE = 0

    fun scheduleNext(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ScreeningReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(nextReminderDelayMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun nextReminderDelayMillis(): Long {
        val now = Calendar.getInstance()
        val reminder = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, REMINDER_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!reminder.after(now)) {
            reminder.add(Calendar.DAY_OF_YEAR, 1)
        }
        return reminder.timeInMillis - now.timeInMillis
    }
}
