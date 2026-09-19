package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * AccessibilityService that enables Java Assistant to perform gestures
 * (scrolling up/down), auto-click on-screen elements, and navigate YouTube Shorts.
 */
class JavaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "JavaAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events can be monitored for context awareness
    }

    override fun onInterrupt() {
        Log.w(TAG, "JavaAccessibilityService interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Simulates a swipe/scroll gesture using dispatchGesture.
     * @param type "scroll_up" or "scroll_down"
     */
    fun scroll(type: String, callback: ((Boolean) -> Unit)? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            callback?.invoke(false)
            return false
        }

        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()

        val path = Path()
        val startX = width * 0.5f
        val endX = width * 0.5f

        val startY: Float
        val endY: Float

        if (type.equals("scroll_up", ignoreCase = true)) {
            // User wants to scroll up (see earlier content): gesture swipes down from top to bottom
            startY = height * 0.30f
            endY = height * 0.75f
        } else {
            // User wants to scroll down (see next content): gesture swipes up from bottom to top
            startY = height * 0.75f
            endY = height * 0.30f
        }

        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val stroke = GestureDescription.StrokeDescription(path, 0, 300L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    callback?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    callback?.invoke(false)
                }
            },
            null
        )
    }

    /**
     * Traverses the active window, finds node matching the given text, and clicks it.
     */
    fun clickNodeWithText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return false

        // 1. First attempt: standard system query
        val foundNodes = root.findAccessibilityNodeInfosByText(cleanText)
        if (!foundNodes.isNullOrEmpty()) {
            for (node in foundNodes) {
                if (performClickOnNodeOrParent(node)) {
                    return true
                }
            }
        }

        // 2. Second attempt: recursive fuzzy search across hierarchy (matches text or contentDescription)
        return searchAndClickNodeRecursive(root, cleanText)
    }

    private fun searchAndClickNodeRecursive(node: AccessibilityNodeInfo, targetText: String): Boolean {
        val nodeText = node.text?.toString().orEmpty()
        val contentDesc = node.contentDescription?.toString().orEmpty()

        if (nodeText.contains(targetText, ignoreCase = true) ||
            contentDesc.contains(targetText, ignoreCase = true)
        ) {
            if (performClickOnNodeOrParent(node)) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (searchAndClickNodeRecursive(child, targetText)) {
                return true
            }
        }
        return false
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) return true
        }

        // Traverse up to closest clickable ancestor
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) return true
            }
            parent = parent.parent
        }
        return false
    }

    /**
     * Opens YouTube Shorts either via deep link or by finding and clicking the Shorts tab.
     */
    fun openYouTubeShorts(): Boolean {
        // Try deep link first
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://shorts/")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback: search for "Shorts" tab/button in active window
            clickNodeWithText("Shorts")
        }
    }

    companion object {
        private const val TAG = "JavaAccessibility"
        var instance: JavaAccessibilityService? = null
        val isServiceEnabled: Boolean get() = instance != null
    }
}
