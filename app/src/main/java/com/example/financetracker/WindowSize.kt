// WindowSize.kt
package com.example.financetracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    Compact,  // Phones in portrait
    Medium,   // Phones in landscape, small tablets
    Expanded  // Large tablets, desktops
}

data class WindowSize(
    val width: WindowSizeClass,
    val height: WindowSizeClass
)

@Composable
fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    return WindowSize(
        width = getWindowSizeClass(screenWidth),
        height = getWindowSizeClass(screenHeight)
    )
}

fun getWindowSizeClass(size: Dp): WindowSizeClass {
    return when {
        size < 600.dp -> WindowSizeClass.Compact
        size < 840.dp -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
}