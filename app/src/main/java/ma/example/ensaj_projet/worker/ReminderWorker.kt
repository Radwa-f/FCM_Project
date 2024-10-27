package ma.example.ensaj_projet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val title = inputData.getString("TITLE") ?: "Reminder"
        val message = inputData.getString("MESSAGE") ?: "You have a reminder."

        // Show notification
        showNotification(title, message)

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(applicationContext, "FCM_CHANNEL")
            .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "FCM_CHANNEL",
                "Reminder Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
