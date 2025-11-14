package com.voicemode

import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class VoiceModeAccessibilityServiceTest {

    private lateinit var service: VoiceModeAccessibilityService

    @Mock
    private lateinit var nodeInfo: AccessibilityNodeInfo

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = VoiceModeAccessibilityService()
    }

    @Test
    fun `isEditable returns true for editable and focusable node`() {
        whenever(nodeInfo.isEditable).thenReturn(true)
        whenever(nodeInfo.isFocusable).thenReturn(true)
        assertTrue(isEditable(nodeInfo))
    }

    @Test
    fun `isEditable returns false for non-editable node`() {
        whenever(nodeInfo.isEditable).thenReturn(false)
        whenever(nodeInfo.isFocusable).thenReturn(true)
        assertFalse(isEditable(nodeInfo))
    }

    @Test
    fun `isEditable returns false for non-focusable node`() {
        whenever(nodeInfo.isEditable).thenReturn(true)
        whenever(nodeInfo.isFocusable).thenReturn(false)
        assertFalse(isEditable(nodeInfo))
    }

    @Test
    fun `isEditable returns false for null node`() {
        assertFalse(isEditable(null))
    }

    private fun isEditable(node: AccessibilityNodeInfo?): Boolean {
        // This is a private method, so we are testing it indirectly.
        // We'll create a public method in the test class that calls the private method.
        val method = service.javaClass.getDeclaredMethod("isEditable", AccessibilityNodeInfo::class.java)
        method.isAccessible = true
        return method.invoke(service, node) as Boolean
    }
}
