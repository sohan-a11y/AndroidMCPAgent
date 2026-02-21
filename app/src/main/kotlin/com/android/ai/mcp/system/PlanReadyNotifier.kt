package com.android.ai.mcp.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.android.ai.mcp.R
import com.android.ai.mcp.ui.MainActivity

object PlanReadyNotifier {

    private const val CHANNEL_ID = "mcp_plan_ready_channel"
    private const val NOTIFICATION_ID = 5151

    fun show(context: Context, commandText: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.plan_ready_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.plan_ready_channel_description)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val confirmIntent = Intent(context, PlanNotificationActionReceiver::class.java).apply {
            action = PlanNotificationActionReceiver.ACTION_CONFIRM_PLAN
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            confirmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(context, PlanNotificationActionReceiver::class.java).apply {
            action = PlanNotificationActionReceiver.ACTION_CANCEL_PLAN
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.plan_ready_title))
            .setContentText(context.getString(R.string.plan_ready_text, commandText.take(48)))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Confirm", confirmPendingIntent)
            .addAction(0, "Cancel", cancelPendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    fun dismiss(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
