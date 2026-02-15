package com.android.mcp.agent.commands.handlers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import com.android.mcp.agent.commands.CommandHandler
import com.android.mcp.agent.commands.CommandResult
import com.android.mcp.agent.permissions.PermissionManager
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Returns current device state information.
 *
 * Result:
 * - "battery_level": Battery percentage (0-100)
 * - "battery_charging": Whether device is charging
 * - "network_type": "wifi", "cellular", "none"
 * - "network_connected": Boolean
 * - "device_model": Device model name
 * - "android_version": Android SDK version
 * - "screen_brightness": Current brightness (0-255)
 */
class DeviceStateHandler(
    private val context: Context
) : CommandHandler {

    override val action = "get_device_state"
    override val requiredPermission = PermissionManager.PERM_DEVICE_INFO

    override suspend fun execute(params: Map<String, JsonElement>): CommandResult {
        val data = mutableMapOf<String, JsonElement>()

        // Battery
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            data["battery_level"] = JsonPrimitive(batteryPct)
            data["battery_charging"] = JsonPrimitive(isCharging)
        }

        // Network
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }

        val networkConnected = capabilities != null
        val networkType = when {
            capabilities == null -> "none"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }

        data["network_connected"] = JsonPrimitive(networkConnected)
        data["network_type"] = JsonPrimitive(networkType)

        // Device info
        data["device_model"] = JsonPrimitive("${Build.MANUFACTURER} ${Build.MODEL}")
        data["android_version"] = JsonPrimitive(Build.VERSION.SDK_INT)
        data["android_release"] = JsonPrimitive(Build.VERSION.RELEASE)

        // Screen brightness
        try {
            val brightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            data["screen_brightness"] = JsonPrimitive(brightness)
        } catch (_: Exception) {
            data["screen_brightness"] = JsonPrimitive(-1)
        }

        return CommandResult.Success(data)
    }
}
