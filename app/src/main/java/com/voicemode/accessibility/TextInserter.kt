package com.voicemode.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber

/**
 * Handles inserting processed text into the currently focused text field.
 */
class TextInserter(
    private val service: AccessibilityService
) {

    /**
     * Insert text into the currently focused editable field.
     *
     * @param text The text to insert
     * @return true if insertion was successful, false otherwise
     */
    fun insertText(text: String): Boolean {
        val rootNode = service.rootInActiveWindow
        if (rootNode == null) {
            Timber.e("No active window found")
            return false
        }

        val focusedNode = findFocusedEditableNode(rootNode)
        if (focusedNode == null) {
            Timber.e("No focused editable node found")
            rootNode.recycle()
            return false
        }

        val success = performTextInsertion(focusedNode, text)

        focusedNode.recycle()
        rootNode.recycle()

        return success
    }

    /**
     * Find the currently focused editable text field
     */
    private fun findFocusedEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // First, try to find the node with input focus
        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null && focusedNode.isEditable) {
            Timber.d("Found focused editable node: ${focusedNode.className}")
            return focusedNode
        }

        // Fallback: search for any focused editable node
        return findEditableNode(root)
    }

    /**
     * Recursively search for an editable text field
     */
    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableNode(child)
            child.recycle()
            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * Perform the actual text insertion using AccessibilityNodeInfo actions
     */
    private fun performTextInsertion(node: AccessibilityNodeInfo, text: String): Boolean {
        // Get current text
        val currentText = node.text?.toString() ?: ""
        Timber.d("Current text: '$currentText'")

        // Append new text to existing text
        val newText = if (currentText.isEmpty()) {
            text
        } else {
            "$currentText $text"
        }

        Timber.d("Inserting text: '$newText'")

        // Create arguments bundle for ACTION_SET_TEXT
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            newText
        )

        // Perform the action
        val success = node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            arguments
        )

        if (success) {
            Timber.i("Text insertion successful")
        } else {
            Timber.e("Text insertion failed via ACTION_SET_TEXT")

            // Fallback: Try using clipboard (less preferred)
            return fallbackClipboardInsertion(text)
        }

        return success
    }

    /**
     * Fallback method: Insert text using clipboard
     * Used when ACTION_SET_TEXT fails (some apps don't support it)
     */
    private fun fallbackClipboardInsertion(text: String): Boolean {
        try {
            val clipboard = service.getSystemService(AccessibilityService.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager

            val clip = android.content.ClipData.newPlainText("Voice Mode", text)
            clipboard.setPrimaryClip(clip)

            Timber.d("Text copied to clipboard as fallback")

            // TODO: We could try to paste automatically here
            // For now, just notify that text is in clipboard

            return true
        } catch (e: Exception) {
            Timber.e(e, "Clipboard fallback failed")
            return false
        }
    }

    /**
     * Replace selected text (if any) with new text
     */
    fun replaceSelectedText(text: String): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        val focusedNode = findFocusedEditableNode(rootNode) ?: run {
            rootNode.recycle()
            return false
        }

        // For now, just insert - selection handling will be added later
        val success = performTextInsertion(focusedNode, text)

        focusedNode.recycle()
        rootNode.recycle()

        return success
    }
}
