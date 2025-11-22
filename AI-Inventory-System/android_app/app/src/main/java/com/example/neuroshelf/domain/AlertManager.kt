package com.example.neuroshelf.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlertManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "neuroshelf_alerts"
        private const val CHANNEL_NAME = "NeuroShelf Alert Notifications"
        private const val CHANNEL_DESCRIPTION = "Alertas sobre eventos críticos, robos o uso no autorizado."
    }

    init {
        createNotificationChannel()
    }

    /**
     * Envía una alerta local al dispositivo.
     */
    fun sendLocalAlert(title: String, body: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Se cierra al tocar
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Crea el canal de notificaciones solo para Android 8+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
