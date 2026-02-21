package com.android.ai.mcp.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PlanNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CONFIRM_PLAN = "com.android.ai.mcp.notification.CONFIRM_PLAN"
        const val ACTION_CANCEL_PLAN = "com.android.ai.mcp.notification.CANCEL_PLAN"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val broadcast = Intent(action).apply {
            `package` = context.packageName
        }
        context.sendBroadcast(broadcast)
    }
}
