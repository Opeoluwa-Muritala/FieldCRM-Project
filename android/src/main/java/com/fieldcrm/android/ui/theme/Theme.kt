package com.fieldcrm.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
)

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

// Desktop-matched Light Mode (Warm cream background, pure white cards, brand text)
val LightFieldColors = FieldColors(
    isLight = true,
    
    gray950 = Color(0xFFF6F4F1), // Warm cream background
    gray900 = Color(0xFFFFFFFF), // Pure white cards
    gray850 = Color(0xFFFBFBFA), // Secondary card surfaces
    gray800 = Color(0xFFE3DFE7), // Light dividers
    gray700 = Color(0xFFCDC7D5), // Input borders
    gray600 = Color(0xFFB4ABC1),
    gray500 = Color(0xFF867D95), // Muted labels
    gray400 = Color(0xFF635973), // Medium text
    gray300 = Color(0xFF423753), // Secondary body text
    gray100 = Color(0xFF281C3D), // Deep brand primary text

    purple950 = Color(0xFFF9F6FC),
    purple900 = Color(0xFFEDE5FA),
    purple700 = Color(0xFFCBB6F2),
    purple600 = Color(0xFF6B3FA0), // Primary LO accent
    purple500 = Color(0xFF7E4FBA),
    purple400 = Color(0xFF986CD1),
    purple200 = Color(0xFFF0E8FA),
    purple100 = Color(0xFFF8F4FD),

    statusSuccess = Color(0xFF2A7C4C),
    statusWarning = Color(0xFFB87820),
    statusDanger = Color(0xFFAD3333),
    statusInfo = Color(0xFF265A96)
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
        fontFamily = FontFamily.Serif, // Premium serif displays
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 32.sp
    ),
    title = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = (-0.3).sp,
        lineHeight = 24.sp
    ),
    body = TextStyle(
        fontFamily = FontFamily.SansSerif, // Refined sans-serif body
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),
    bodyStrong = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),
    label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        lineHeight = 16.sp
    ),
    mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
        lineHeight = 18.sp
    )
)

// ==========================================
// SHAPES SYSTEM
// ==========================================

@Immutable
data class FieldShapes(
    val inputRadius: Dp = 8.dp,
    val cardRadius: Dp = 12.dp,
    val sheetRadius: Dp = 18.dp
)

val LocalFieldColors = staticCompositionLocalOf { DarkFieldColors }
val LocalFieldTypography = staticCompositionLocalOf { FieldTypo }
val LocalFieldShapes = staticCompositionLocalOf { FieldShapes() }

// Dynamic theme resolution base on user role and system mode
fun getRoleColors(role: UserRole?, darkTheme: Boolean): FieldColors {
    val baseUrl = "http://127.0.0.1:8000"
    val base = if (darkTheme) DarkFieldColors else LightFieldColors
    
    val (primaryColor, primaryHover, primaryLight, primaryTint) = when (role) {
        UserRole.CREDIT_OFFICER -> if (darkTheme) {
            listOf(Color(0xFF287C52), Color(0xFF389264), Color(0xFF6BCF9A), Color(0xFF1E2F26))
        } else {
            listOf(Color(0xFF1A6B45), Color(0xFF238C5A), Color(0xFF6BCF9A), Color(0xFFE8FAF0))
        }
        UserRole.BRANCH_MANAGER -> if (darkTheme) {
            listOf(Color(0xFFC07B1E), Color(0xFFD38D30), Color(0xFFD4A46B), Color(0xFF2E2417))
        } else {
            listOf(Color(0xFFA05A00), Color(0xFFC07000), Color(0xFFD4A46B), Color(0xFFFFF5E8))
        }
        UserRole.AUDITOR -> if (darkTheme) {
            listOf(Color(0xFF6C7C96), Color(0xFF7E8EAA), Color(0xFFBAC5D6), Color(0xFF22262E))
        } else {
            listOf(Color(0xFF4A5568), Color(0xFF636D7F), Color(0xFFA0AEC0), Color(0xFFEDF2F7))
        }
        UserRole.ADMIN_MCR -> if (darkTheme) {
            listOf(Color(0xFF4E2C80), Color(0xFF633D9E), Color(0xFF9E7BD6), Color(0xFF241933))
        } else {
            listOf(Color(0xFF2D1A4A), Color(0xFF3D2A5A), Color(0xFF8B6BB0), Color(0xFFEDE8F4))
        }
        else -> if (darkTheme) {
            listOf(Color(0xFF7B52B3), Color(0xFF8E63C7), Color(0xFFA67EDB), Color(0xFF1E152A))
        } else {
            listOf(Color(0xFF6B3FA0), Color(0xFF7E4FBA), Color(0xFF986CD1), Color(0xFFF0E8FA))
        }
    }
    
    return base.copy(
        purple950 = primaryTint,
        purple900 = primaryTint,
        purple700 = primaryHover,
        purple600 = primaryColor,
        purple500 = primaryColor,
        purple400 = primaryLight,
        purple200 = primaryTint,
        purple100 = primaryTint
    )
}

@Composable
fun FieldCRMTheme(
    role: UserRole? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = getRoleColors(role, darkTheme)
    
    val m3ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.purple600,
            onPrimary = Color.White,
            background = colors.gray950,
            onBackground = colors.gray300,
            surface = colors.gray900,
            onSurface = colors.gray300,
            surfaceVariant = colors.gray850,
            onSurfaceVariant = colors.gray400,
            outline = colors.gray700,
            outlineVariant = colors.gray600,
            error = colors.statusDanger,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = colors.purple600,
            onPrimary = Color.White,
            background = colors.gray950,
            onBackground = colors.gray300,
            surface = colors.gray900,
            onSurface = colors.gray300,
            surfaceVariant = colors.gray850,
            onSurfaceVariant = colors.gray400,
            outline = colors.gray700,
            outlineVariant = colors.gray600,
            error = colors.statusDanger,
            onError = Color.White
        )
    }

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
