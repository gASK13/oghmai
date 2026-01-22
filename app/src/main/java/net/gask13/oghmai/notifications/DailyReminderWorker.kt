package net.gask13.oghmai.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.gask13.oghmai.preferences.PreferencesManager

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val preferencesManager = PreferencesManager(applicationContext)
            val language = preferencesManager.getLanguage()

            NotificationHelper.showNotification(applicationContext, language)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
