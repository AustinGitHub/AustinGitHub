package com.example.gymcompanion

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        NotificationHelper.showReminder(applicationContext)
        return Result.success()
    }
}
