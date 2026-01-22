package net.gask13.oghmai.notifications

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val WORK_TAG = "daily_reminder_work"

    fun scheduleDailyNotification(context: Context, hour: Int, minute: Int) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        // Set the time to the user's selected time
        dueDate.set(Calendar.HOUR_OF_DAY, hour)
        dueDate.set(Calendar.MINUTE, minute)
        dueDate.set(Calendar.SECOND, 0)

        // If the selected time has already passed today, schedule for tomorrow
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )
    }

    fun cancelDailyNotification(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }
}
