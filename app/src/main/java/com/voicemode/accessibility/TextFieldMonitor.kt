package com.voicemode.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber

/**
 * Monitors focused text fields and tracks their state.
 */
class TextFieldMonitor {

    private var currentTextField: AccessibilityNodeInfo? = null
    private var currentPackage: String? = null
    private var currentText: String = ""

    fun onTextFieldFocused(node: AccessibilityNodeInfo) {
        // Recycle previous node
        currentTextField?.recycle()

        // Store new focused field
        currentTextField = AccessibilityNodeInfo.obtain(node)
        currentText = node.text?.toString() ?: ""

        Timber.d("Text field focused - Package: ${node.packageName}, Class: ${node.className}")
        Timber.d("Current text: '$currentText'")
    }

    fun onTextChanged(event: AccessibilityEvent) {
        currentText = event.text.joinToString("")
        Timber.v("Text changed: '$currentText'")
    }

    fun getCurrentTextField(): AccessibilityNodeInfo? {
        return currentTextField
    }

    fun getCurrentText(): String {
        return currentText
    }

    fun getCurrentPackage(): String? {
        return currentPackage
    }

    /**
     * Check if there's a valid text field currently focused
     */
    fun hasValidTextField(): Boolean {
        val field = currentTextField
        return field != null && field.isEditable && !field.isPassword
    }

    fun cleanup() {
        currentTextField?.recycle()
        currentTextField = null
    }
}
