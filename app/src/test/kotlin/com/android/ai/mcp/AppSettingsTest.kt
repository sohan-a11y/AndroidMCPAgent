package com.android.ai.mcp

import com.android.ai.mcp.domain.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun maxPlanSteps_defaultsToTwenty() {
        val settings = AppSettings()
        assertEquals(20, settings.maxPlanSteps)
        assertEquals("moonshotai/kimi-k2.5:free", settings.openRouterModelId)
        assertEquals("moonshotai/kimi-k2.5", settings.nvidiaModelId)
        assertEquals("AI", settings.wakeWord)
        assertEquals(5, settings.vaultSessionTimeoutMinutes)
    }

    @Test
    fun maxPlanSteps_clampsToRange() {
        assertEquals(1, AppSettings.sanitizeMaxPlanSteps(0))
        assertEquals(50, AppSettings.sanitizeMaxPlanSteps(100))
    }

    @Test
    fun vaultTimeout_clampsToRange() {
        assertEquals(1, AppSettings.sanitizeVaultSessionTimeoutMinutes(0))
        assertEquals(30, AppSettings.sanitizeVaultSessionTimeoutMinutes(100))
    }
}
