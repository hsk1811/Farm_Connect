package com.farmconnect.app.ui.utils

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

/**
 * Prevents screenshots and screen recordings for the composable where it's used.
 * This is commonly used for sensitive information like contracts, payment details, etc.
 * 
 * Usage:
 * ```
 * @Composable
 * fun MySecureScreen() {
 *     PreventScreenCapture()
 *     // Your screen content
 * }
 * ```
 */
@Composable
fun PreventScreenCapture() {
    val view = LocalView.current
    
    DisposableEffect(Unit) {
        val window = ViewCompat.getWindowInsetsController(view)?.let {
            val activity = view.context as? android.app.Activity
            activity?.window
        }
        
        // Set FLAG_SECURE to prevent screenshots and screen recordings
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        onDispose {
            // Clear FLAG_SECURE when composable is disposed
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
