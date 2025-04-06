package com.colbycoapps.med_standards.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.colbycoapps.med_standards.R
import com.colbycoapps.med_standards.SplashActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "fcm_default_channel"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Перевіряємо чи повідомлення містить payload із сповіщенням
        remoteMessage.notification?.let {
            val title = it.title ?: ""
            val body = it.body ?: ""
            Log.d("FCM", "Notification received: title=$title, body=$body")
            showNotification(title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_ONE_SHOT
        )

        // Створюємо NotificationCompat.Builder
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.img) // Замініть на свою іконку
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Створюємо NotificationChannel, якщо Android O+
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Main message channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for receiving messages from FCM"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Відображаємо сповіщення
        notificationManager.notify(0, builder.build())
    }
}
