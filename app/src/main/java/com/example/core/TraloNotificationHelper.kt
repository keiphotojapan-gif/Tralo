package com.example.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.model.Trip

object TraloNotificationHelper {
    private const val CHANNEL_ID = "tralo_travel_reminders"
    private const val CHANNEL_NAME = "TRALO 旅行・交通リマインダー"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "出発時刻、列車発車、ホテルチェックイン等のリマインダー通知"
                }
                manager?.createNotificationChannel(channel)
            }
        }
    }

    fun showTripNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        runCatching {
            manager.notify(notificationId, builder.build())
        }
    }

    fun showTripReminder(context: Context, trip: Trip) {
        val title = "旅程リマインダー: ${trip.title}"
        val message = "${trip.startDate} から ${trip.destination} へのご旅行予定です。出発準備と切符・ホテルのご確認をどうぞ。"
        showTripNotification(context, trip.id.hashCode(), title, message)
    }
}
