// PhoneScreenSize.kt
package com.example.financetracker

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PhoneScreenSize {
    Small,   // Small phones (< 360dp width)
    Regular, // Regular phones (360dp - 400dp)
    Large    // Large phones (> 400dp)
}

@Composable
fun rememberPhoneScreenSize(): PhoneScreenSize {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    return when {
        screenWidth < 360.dp -> PhoneScreenSize.Small
        screenWidth < 400.dp -> PhoneScreenSize.Regular
        else -> PhoneScreenSize.Large
    }
}

@Composable
fun getResponsivePadding(): Dp {
    val screenSize = rememberPhoneScreenSize()
    return when (screenSize) {
        PhoneScreenSize.Small -> 12.dp
        PhoneScreenSize.Regular -> 16.dp
        PhoneScreenSize.Large -> 20.dp
    }
}

@Composable
fun getResponsiveSpacing(): Dp {
    val screenSize = rememberPhoneScreenSize()
    return when (screenSize) {
        PhoneScreenSize.Small -> 8.dp
        PhoneScreenSize.Regular -> 12.dp
        PhoneScreenSize.Large -> 16.dp
    }
}