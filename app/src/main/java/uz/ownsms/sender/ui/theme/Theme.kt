package uz.ownsms.sender.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Ultramarine,
    onPrimary = Color.White,
    secondary = LiveGreen,
    onSecondary = Color.White,
    background = Cloud,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECEEF4),
    onSurfaceVariant = StatusMuted,
    outlineVariant = Color(0xFFDFE3EB),
    error = StatusFailed,
)

private val DarkColors = darkColorScheme(
    primary = UltramarineLight,
    onPrimary = Ink,
    secondary = LiveGreen,
    onSecondary = Ink,
    background = Ink,
    onBackground = Cloud,
    surface = InkSurface,
    onSurface = Cloud,
    surfaceVariant = Color(0xFF222834),
    onSurfaceVariant = Color(0xFF9AA4B2),
    outlineVariant = Color(0xFF2A313C),
    error = StatusFailed,
)

@Composable
fun OwnSmsTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = OwnSmsTypography,
        content = content,
    )
}
