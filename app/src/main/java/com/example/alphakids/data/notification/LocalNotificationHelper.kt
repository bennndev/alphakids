package com.example.alphakids.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.alphakids.MainActivity
import com.example.alphakids.R

/**
 * Helper para gestionar notificaciones locales en la app.
 */
object LocalNotificationHelper {

    private const val CHANNEL_ID = "alphakids_word_assignments"
    private const val CHANNEL_NAME = "Asignación de Palabras"
    private const val CHANNEL_DESCRIPTION = "Notificaciones cuando se asignan nuevas palabras"

    /**
     * Crea el canal de notificaciones (necesario para Android 8.0+).
     * Debe llamarse una vez al inicio de la app.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación local cuando se asigna una palabra.
     */
    fun showWordAssignmentNotification(
        context: Context,
        wordText: String,
        studentName: String
    ) {
        // Intent para abrir la app al tocar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Construir la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("📚 Nueva palabra asignada")
            .setContentText("Se asignó la palabra \"$wordText\" a $studentName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Mostrar la notificación
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
