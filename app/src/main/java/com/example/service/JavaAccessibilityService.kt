package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.net.URLEncoder

/**
 * AccessibilityService that enables Java Assistant to perform gestures
 * (scrolling up/down/left/right), auto-click on-screen elements, auto-send WhatsApp messages,
 * lock screen, and trigger native system navigation (back, home, recents, notifications, screenshot).
 */
class JavaAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isWaitingForWhatsAppSend = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "JavaAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // WhatsApp Automatic Send Dispatcher
        if (isWaitingForWhatsAppSend && event.packageName == "com.whatsapp") {
            mainHandler.postDelayed({
                val sent = findAndClickWhatsAppSendButton()
                if (sent) {
                    isWaitingForWhatsAppSend = false
                    Log.i(TAG, "WhatsApp Send button automatically clicked")
                }
            }, 600L)
        }
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

    /* -------------------------------------------------------------
     * SMART SCROLLING (Up, Down, Left, Right)
     * ------------------------------------------------------------- */
    fun scroll(type: String, callback: ((Boolean) -> Unit)? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            callback?.invoke(false)
            return false
        }

        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()

        val path = Path()
        val startX: Float
        val startY: Float
        val endX: Float
        val endY: Float

        when (type.lowercase().trim()) {
            "scroll_up", "up" -> {
                // Drag down to reveal content above
                startX = width * 0.5f
                endX = width * 0.5f
                startY = height * 0.25f
                endY = height * 0.75f
            }
            "scroll_left", "left" -> {
                // Drag right to left
                startX = width * 0.85f
                endX = width * 0.15f
                startY = height * 0.5f
                endY = height * 0.5f
            }
            "scroll_right", "right" -> {
                // Drag left to right
                startX = width * 0.15f
                endX = width * 0.85f
                startY = height * 0.5f
                endY = height * 0.5f
            }
            else -> {
                // scroll_down (default): drag up to reveal content below
                startX = width * 0.5f
                endX = width * 0.5f
                startY = height * 0.75f
                endY = height * 0.25f
            }
        }

        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val stroke = GestureDescription.StrokeDescription(path, 0, 250L)
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

    /* -------------------------------------------------------------
     * SMART CLICK (Recursive text search + Clickable Parent Traversal)
     * ------------------------------------------------------------- */
    fun clickNodeWithText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return false

        // Special check for Send button (WhatsApp / SMS / Messenger)
        if (cleanText.equals("Send", ignoreCase = true) || cleanText.equals("भेजें", ignoreCase = true)) {
            if (findAndClickWhatsAppSendButton()) {
                return true
            }
        }

        // 1. First attempt: standard system query
        val foundNodes = root.findAccessibilityNodeInfosByText(cleanText)
        if (!foundNodes.isNullOrEmpty()) {
            for (node in foundNodes) {
                if (performClickOnNodeOrParent(node)) {
                    return true
                }
            }
        }

        // 2. Second attempt: comma-separated multi-label fallback (e.g. "Send,भेजें")
        if (cleanText.contains(",")) {
            val alternatives = cleanText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (alt in alternatives) {
                if (searchAndClickNodeRecursive(root, alt)) {
                    return true
                }
            }
        }

        // 3. Third attempt: recursive fuzzy search across hierarchy (matches text or contentDescription)
        return searchAndClickNodeRecursive(root, cleanText)
    }

    /* -------------------------------------------------------------
     * TEXT INPUT & ENTER DISPATCH
     * ------------------------------------------------------------- */
    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false

        // 1. Try to find focused input node
        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null && setTextOnNode(focusedNode, text)) {
            return true
        }

        // 2. Try to find any editable node in the active window
        val editable = findFirstEditableNode(root)
        if (editable != null && setTextOnNode(editable, text)) {
            return true
        }

        return false
    }

    private fun setTextOnNode(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        if (!success) {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return success
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    fun pressEnter(): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            // 1. Try ACTION_IME_ENTER on focused input (Android 11+)
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focused != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (focused.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)) {
                    return true
                }
            }

            // 2. Try clicking search or enter button if visible
            val searchTerms = listOf("Search", "खोजें", "Enter", "Go", "Done", "Submit")
            for (term in searchTerms) {
                val nodes = root.findAccessibilityNodeInfosByText(term)
                if (!nodes.isNullOrEmpty()) {
                    for (n in nodes) {
                        if (performClickOnNodeOrParent(n)) return true
                    }
                }
            }
        }

        // 3. Fallback: tap bottom-right corner where soft keyboard Enter/Search key lives
        return tapBottomRightEnter()
    }

    private fun tapBottomRightEnter(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()

        val x = width * 0.90f
        val y = height * 0.94f

        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
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

    /* -------------------------------------------------------------
     * GLOBAL SYSTEM ACTIONS
     * ------------------------------------------------------------- */
    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            Log.w(TAG, "GLOBAL_ACTION_LOCK_SCREEN requires Android 9 (API 28)+")
            false
        }
    }

    fun performGlobalBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performGlobalHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performOpenNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun performOpenRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    /* -------------------------------------------------------------
     * WHATSAPP AUTOMATION
     * ------------------------------------------------------------- */
    fun triggerWhatsAppSend(context: Context, contactOrPhone: String, message: String) {
        isWaitingForWhatsAppSend = true

        val intent = Intent(Intent.ACTION_VIEW).apply {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val cleanNumber = contactOrPhone.replace(Regex("[^0-9+]"), "")
            data = if (cleanNumber.length >= 10) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMsg")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
            }
            `package` = "com.whatsapp"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp launch failed", e)
            isWaitingForWhatsAppSend = false
        }
    }

    fun findAndClickWhatsAppSendButton(): Boolean {
        val root = rootInActiveWindow ?: return false
        return searchAndClickWhatsAppSend(root)
    }

    private fun searchAndClickWhatsAppSend(node: AccessibilityNodeInfo): Boolean {
        val desc = node.contentDescription?.toString().orEmpty()
        val text = node.text?.toString().orEmpty()
        val viewId = node.viewIdResourceName.orEmpty()

        if (desc.equals("Send", ignoreCase = true) ||
            desc.contains("Send", ignoreCase = true) ||
            desc.contains("भेजें", ignoreCase = true) ||
            text.contains("Send", ignoreCase = true) ||
            viewId.contains("send", ignoreCase = true)
        ) {
            if (performClickOnNodeOrParent(node)) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (searchAndClickWhatsAppSend(child)) {
                return true
            }
        }
        return false
    }

    fun openYouTubeShorts(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://shorts/")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            true
        } catch (e: Exception) {
            clickNodeWithText("Shorts")
        }
    }

    companion object {
        private const val TAG = "JavaAccessibility"
        var instance: JavaAccessibilityService? = null
        val isServiceEnabled: Boolean get() = instance != null
    }
}
