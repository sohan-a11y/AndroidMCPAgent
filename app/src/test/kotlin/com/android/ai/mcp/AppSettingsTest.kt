package com.android.ai.mcp

import com.android.ai.mcp.domain.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun maxPlanSteps_defaultsToTwenty() {
        val settings = AppSettings()
        assertEquals(20, settings.maxPlanSteps)
    }

    @Test
    fun maxPlanSteps_clampsToRange() {
        assertEquals(1, AppSettings.sanitizeMaxPlanSteps(0))
        assertEquals(50, AppSettings.sanitizeMaxPlanSteps(100))
    }
}
