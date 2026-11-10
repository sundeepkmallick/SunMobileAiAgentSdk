package com.sunmobile.aiagent.mlkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiNanoConfigurationTest {
    @Test
    fun defaultsEnableHostAppFunctionsAndRequireAndroid16() {
        val configuration = GeminiNanoConfiguration(instructions = "Follow host rules.")
        assertEquals(36, configuration.minimumAndroidApi)
        assertEquals(10, configuration.maxTurns)
        assertTrue(configuration.enableAppFunctions)
    }

    @Test
    fun appFunctionsCanBeDisabledExplicitly() {
        val configuration = GeminiNanoConfiguration(
            instructions = "Respond without tools.",
            enableAppFunctions = false,
        )
        assertFalse(configuration.enableAppFunctions)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankInstructionsAreRejected() {
        GeminiNanoConfiguration(instructions = " ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonPositiveTurnLimitIsRejected() {
        GeminiNanoConfiguration(instructions = "Use host tools.", maxTurns = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun androidApiBelowLibraryMinimumIsRejected() {
        GeminiNanoConfiguration(instructions = "Use host tools.", minimumAndroidApi = 25)
    }
}
