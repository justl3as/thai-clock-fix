package com.thaiclockfix.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Material 3 Expressive Typography Hierarchy
// (font family is intentionally omitted — SansSerif is the platform default)
val Typography =
    Typography(
        displayLarge =
        TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 80.sp,
            lineHeight = 84.sp,
            letterSpacing = (-1).sp,
        ),
        displayMedium =
        TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp,
            lineHeight = 48.sp,
            letterSpacing = 0.sp,
        ),
        displaySmall =
        TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
        ),
        headlineLarge =
        TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium =
        TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall =
        TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp,
        ),
        titleLarge =
        TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 25.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium =
        TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall =
        TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge =
        TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.25.sp,
        ),
        bodyMedium =
        TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall =
        TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge =
        TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium =
        TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall =
        TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.5.sp,
        ),
    )
