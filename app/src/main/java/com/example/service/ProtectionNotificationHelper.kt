package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object ProtectionNotificationHelper {
  const val CHANNEL_PROTECTION = "betshield_protection"
  const val CHANNEL_ALERTS = "betshield_alerts"
  const val VPN_NOTIFICATION_ID = 1001
  private const val ALERT_NOTIFICATION_BASE_ID = 2000

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(NotificationManager::class.java) ?: return
      val protectionChannel =
        NotificationChannel(
            CHANNEL_PROTECTION,
            context.getString(R.string.channel_protection_name),
            NotificationManager.IMPORTANCE_LOW,
          )
          .apply {
            description = context.getString(R.string.channel_protection_desc)
            setShowBadge(false)
          }
      val alertsChannel =
        NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.channel_alerts_name),
            NotificationManager.IMPORTANCE_DEFAULT,
          )
          .apply { description = context.getString(R.string.channel_alerts_desc) }
      manager.createNotificationChannel(protectionChannel)
      manager.createNotificationChannel(alertsChannel)
    }
  }

  fun buildVpnForegroundNotification(context: Context, activeDomainCount: Int): Notification {
    ensureChannels(context)
    val openAppIntent =
      PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val stopIntent =
      PendingIntent.getService(
        context,
        1,
        Intent(context, BetShieldVpnService::class.java).apply {
          action = BetShieldVpnService.ACTION_STOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat.Builder(context, CHANNEL_PROTECTION)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle("BetShield Protection Active")
      .setContentText("Local DNS shield monitoring $activeDomainCount blocked domains")
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(openAppIntent)
      .addAction(0, "Pause", stopIntent)
      .build()
  }

  fun showBlockAlert(context: Context, title: String, message: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }
    ensureChannels(context)
    val openIntent =
      PendingIntent.getActivity(
        context,
        2,
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ALERTS)
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setAutoCancel(true)
        .setContentIntent(openIntent)
        .build()

    NotificationManagerCompat.from(context)
      .notify(ALERT_NOTIFICATION_BASE_ID + (title.hashCode() and 0xFF), notification)
  }
}
