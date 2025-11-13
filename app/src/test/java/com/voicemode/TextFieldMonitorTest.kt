package com.voicemode

import com.voicemode.accessibility.TextFieldMonitor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TextFieldMonitor
 */
class TextFieldMonitorTest {

    private lateinit var monitor: TextFieldMonitor

    @Before
    fun setup() {
        monitor = TextFieldMonitor()
    }

    @After
    fun teardown() {
        monitor.cleanup()
    }

    @Test
    fun `getCurrentText returns empty string initially`() {
        assertEquals("", monitor.getCurrentText())
    }

    @Test
    fun `hasValidTextField returns false when no field is set`() {
        assertFalse(monitor.hasValidTextField())
    }

    @Test
    fun `getCurrentTextField returns null initially`() {
        assertNull(monitor.getCurrentTextField())
    }

    // Note: More comprehensive tests would require mocking AccessibilityNodeInfo
    // which is complex. Integration tests would be better for full testing.
}
