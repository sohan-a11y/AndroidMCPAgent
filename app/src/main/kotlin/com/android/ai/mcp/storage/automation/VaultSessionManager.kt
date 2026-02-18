package com.android.ai.mcp.storage.automation

class VaultSessionManager {

    @Volatile
    private var unlockedUntilMillis: Long = 0

    fun isUnlocked(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return nowMillis < unlockedUntilMillis
    }

    fun unlockForMinutes(minutes: Int, nowMillis: Long = System.currentTimeMillis()) {
        val timeoutMillis = minutes.coerceAtLeast(1) * 60_000L
        unlockedUntilMillis = nowMillis + timeoutMillis
    }

    fun lockNow() {
        unlockedUntilMillis = 0
    }
}
