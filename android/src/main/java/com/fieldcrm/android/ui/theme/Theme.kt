package com.fieldcrm.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole

// ==========================================
// COLOR SYSTEM (Role-aware & System-synced)
// ==========================================

@Immutable
data class FieldColors(
    val isLight: Boolean,
    
    // Gray Ramp (surfaces, workspaces, text)
    val gray950: Color, // Background
    val gray900: Color, // Primary Card Surface
    val gray850: Color, // Secondary Card Surface
    val gray800: Color, // Dividers, borders
    val gray700: Color, // Input borders
    val gray600: Color,
    val gray500: Color, // Muted labels
    val gray400: Color, // Medium text
    val gray300: Color, // Secondary body text
    val gray100: Color, // Primary title text

    // Dynamic Accent Ramp (mapped to purple variables for compatibility)
    val purple950: Color,
    val purple900: Color,
    val purple700: Color,
    val purple600: Color, // Primary action color
    val purple500: Color,
    val purple400: Color, // Accent hover/light
    val purple200: Color, // Light background tint
    val purple100: Color,

    // Semantic States
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusDanger: Color,
    val statusInfo: Color
) {
    val brandPrimary: Color get() = purple600
}

// Desktop-inspired Dark Mode (Warm, deep slate-purples)
val DarkFieldColors = FieldColors(
    isLight = false,
    
    gray950 = Color(0xFF0F0E13),
    gray900 = Color(0xFF16151A),
    gray850 = Color(0xFF1F1D24),
    gray800 = Color(0xFF2B2832),
    gray700 = Color(0xFF3A3644),
    gray600 = Color(0xFF4C4759),
    gray500 = Color(0xFF8F889B),
    gray400 = Color(0xFFB1AABF),
    gray300 = Color(0xFFD8D4E2),
    gray100 = Color(0xFFFAF9FB),

    purple950 = Color(0xFF1E152A),
    purple900 = Color(0xFF2D1E42),
    purple700 = Color(0xFF5A3C85),
    purple600 = Color(0xFF7B52B3), // Default LO accent
    purple500 = Color(0xFF8E63C7),
    purple400 = Color(0xFFA67EDB),
    purple200 = Color(0xFFDFD1F5),
    purple100 = Color(0xFFEFE6FA),

    statusSuccess = Color(0xFF4A9066),
    statusWarning = Color(0xFFC08E3E),
    statusDanger = Color(0xFFC04E4E),
    statusInfo = Color(0xFF4C7EB8)
)

// DESIGN.md light tokens mapped directly to the compatibility color ramp used by
// existing FieldCRM components. New screens should consume MaterialTheme roles.
val LightFieldColors = FieldColors(
    isLight = true,
    gray950 = Color(0xFFF2F2F2),
    gray900 = Color(0xFFFFFFFF),
    gray850 = Color(0xFFF2F4F6),
    gray800 = Color(0xFFE0E3E5),
    gray700 = Color(0xFF7D7483),
    gray600 = Color(0xFF625A68),
    gray500 = Color(0xFF4C4451),
    gray400 = Color(0xFF3A3340),
    gray300 = Color(0xFF29242D),
    gray100 = Color(0xFF191C1E),
    purple950 = Color(0xFFF0DBFF),
    purple900 = Color(0xFFE8D5F5),
    purple700 = Color(0xFF4B0082),
    purple600 = Color(0xFF2E0052),
    purple500 = Color(0xFF622599),
    purple400 = Color(0xFF6F2EA5),
    purple200 = Color(0xFFE7C7FF),
    purple100 = Color(0xFFF0DBFF),
    statusSuccess = Color(0xFF087A55),
    statusWarning = Color(0xFF8A5700),
    statusDanger = Color(0xFFBA1A1A),
    statusInfo = Color(0xFF285F9E)
)



// ==========================================
// TYPOGRAPHY SYSTEM (Editorial Serif + Sans)
// ==========================================

@Immutable
data class FieldTypography(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val mono: TextStyle
)

val FieldTypo = FieldTypography(
    display = TextStyle(
        fontFamily = FontFamily.Serif, // Playfair Display Serif displays
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp, // headline-lg
        letterSpacing = (-0.5).sp,
        lineHeight = 40.sp,
        fontFeatureSettings = "tnum"
    ),
    title = TextStyle(
        fontFamily = FontFamily.Serif, // Playfair Display Serif
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, // headline-sm
        letterSpacing = (-0.3).sp,
        lineHeight = 28.sp,
        fontFeatureSettings = "tnum"
    ),
    body = TextStyle(
        fontFamily = FontFamily.SansSerif, // DM Sans SansSerif body
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp, // body-md
        letterSpacing = 0.sp,
        lineHeight = 24.sp,
        fontFeatureSettings = "tnum"
    ),
    bodyStrong = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp, // body-md bold
        letterSpacing = 0.sp,
        lineHeight = 24.sp,
        fontFeatureSettings = "tnum"
    ),
    label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, // label-md
        letterSpacing = 0.5.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    ),
    mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    )
)

// ==========================================
// SHAPES SYSTEM
// ==========================================

@Immutable
data class FieldShapes(
    val inputRadius: Dp = 4.dp, // 4px / 0.25rem corner radius for standard elements
    val cardRadius: Dp = 8.dp,  // 8px / 0.5rem corner radius for containers
    val sheetRadius: Dp = 8.dp
)

val LocalFieldColors = staticCompositionLocalOf { DarkFieldColors }
val LocalFieldTypography = staticCompositionLocalOf { FieldTypo }
val LocalFieldShapes = staticCompositionLocalOf { FieldShapes() }

// Dynamic theme resolution base on user role and system mode
fun getRoleColors(@Suppress("UNUSED_PARAMETER") role: UserRole?, darkTheme: Boolean): FieldColors =
    if (darkTheme) DarkFieldColors else LightFieldColors

@Composable
fun FieldCRMTheme(
    role: UserRole? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = getRoleColors(role, darkTheme)
    
    val m3ColorScheme = if (darkTheme) darkColorScheme(
        primary = colors.purple400, onPrimary = Color(0xFF1D002F),
        primaryContainer = colors.purple900, onPrimaryContainer = colors.purple100,
        secondary = Color(0xFFD5B7DD), onSecondary = Color(0xFF38253D),
        background = colors.gray950, onBackground = colors.gray100,
        surface = colors.gray900, onSurface = colors.gray100,
        surfaceVariant = colors.gray850, onSurfaceVariant = colors.gray300,
        outline = colors.gray500, outlineVariant = colors.gray700,
        error = colors.statusDanger, onError = Color.White
    ) else lightColorScheme(
        primary = Color(0xFF2E0052), onPrimary = Color.White,
        primaryContainer = Color(0xFFF0DBFF), onPrimaryContainer = Color(0xFF2C0050),
        secondary = Color(0xFF89268B), onSecondary = Color.White,
        background = Color(0xFFF2F2F2), onBackground = Color(0xFF191C1E),
        surface = Color.White, onSurface = Color(0xFF191C1E),
        surfaceVariant = Color(0xFFE0E3E5), onSurfaceVariant = Color(0xFF4C4451),
        outline = Color(0xFF7D7483), outlineVariant = Color(0xFFCEC3D3),
        error = Color(0xFFBA1A1A), onError = Color.White
    )

    CompositionLocalProvider(
        LocalFieldColors provides colors,
        LocalFieldTypography provides FieldTypo,
        LocalFieldShapes provides FieldShapes()
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            content = content
        )
    }
}

object FieldTheme {
    val colors: FieldColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFieldColors.current

    val typography: FieldTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalFieldTypography.current

    val shapes: FieldShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalFieldShapes.current
}
