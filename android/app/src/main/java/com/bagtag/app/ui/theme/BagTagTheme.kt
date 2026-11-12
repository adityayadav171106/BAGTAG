package com.bagtag.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bagtag.app.R

private val Ink = Color(0xFFF7F6F1)
private val Canvas = Color(0xFF101412)
private val Surface = Color(0xFF1A211D)
private val SurfaceHigh = Color(0xFF252E28)
private val Signal = Color(0xFFA6FF7A)
private val Muted = Color(0xFFB7C0B9)
private val Danger = Color(0xFFFFB4A9)

private val AmericanCaptain = FontFamily(Font(R.font.american_captain))

private val BagTagTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 52.sp, letterSpacing = 1.5.sp),
    headlineLarge = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 36.sp, letterSpacing = 1.2.sp),
    headlineMedium = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 28.sp, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 22.sp, letterSpacing = .8.sp),
    titleMedium = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 18.sp, letterSpacing = .7.sp),
    titleSmall = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = .7.sp),
    bodyLarge = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 18.sp, letterSpacing = .55.sp),
    bodyMedium = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 15.sp, letterSpacing = .45.sp),
    bodySmall = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = .4.sp),
    labelLarge = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = .9.sp),
    labelSmall = TextStyle(fontFamily = AmericanCaptain, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = .8.sp),
)

@Composable fun BagTagTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Signal, onPrimary = Canvas, background = Canvas, onBackground = Ink,
            surface = Surface, onSurface = Ink, surfaceVariant = SurfaceHigh,
            onSurfaceVariant = Muted, error = Danger,
        ),
        typography = BagTagTypography,
        content = content,
    )
}
