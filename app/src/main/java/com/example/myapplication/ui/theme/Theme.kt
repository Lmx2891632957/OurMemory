package com.example.myapplication.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val WarmLightColorScheme = lightColorScheme(
    primary = WarmPalette.Primary,
    onPrimary = WarmPalette.OnPrimary,
    primaryContainer = WarmPalette.PrimaryContainer,
    onPrimaryContainer = WarmPalette.OnPrimaryContainer,
    secondary = WarmPalette.Secondary,
    onSecondary = WarmPalette.OnSecondary,
    secondaryContainer = WarmPalette.SecondaryContainer,
    onSecondaryContainer = WarmPalette.OnSecondaryContainer,
    tertiary = WarmPalette.Tertiary,
    onTertiary = WarmPalette.OnTertiary,
    tertiaryContainer = WarmPalette.TertiaryContainer,
    onTertiaryContainer = WarmPalette.OnTertiaryContainer,
    background = WarmPalette.Background,
    onBackground = WarmPalette.TextPrimary,
    surface = WarmPalette.Surface,
    onSurface = WarmPalette.TextPrimary,
    surfaceVariant = WarmPalette.SurfaceVariant,
    onSurfaceVariant = WarmPalette.TextSecondary,
    outline = WarmPalette.Outline,
    outlineVariant = WarmPalette.OutlineVariant,
    error = WarmPalette.Error,
    onError = WarmPalette.OnError,
    errorContainer = WarmPalette.ErrorContainer,
    onErrorContainer = WarmPalette.OnErrorContainer,
    surfaceTint = WarmPalette.Primary,
    inverseSurface = WarmPalette.InverseSurface,
    inverseOnSurface = WarmPalette.InverseOnSurface,
    inversePrimary = WarmPalette.InversePrimary,
    scrim = Color.Black,
)

private val WarmDarkColorScheme = darkColorScheme(
    primary = WarmPalette.DarkPrimary,
    onPrimary = WarmPalette.DarkOnPrimary,
    primaryContainer = WarmPalette.DarkPrimaryContainer,
    onPrimaryContainer = WarmPalette.DarkOnPrimaryContainer,
    secondary = WarmPalette.DarkSecondary,
    onSecondary = WarmPalette.DarkOnSecondary,
    secondaryContainer = WarmPalette.DarkSecondaryContainer,
    onSecondaryContainer = WarmPalette.DarkOnSecondaryContainer,
    tertiary = WarmPalette.DarkTertiary,
    onTertiary = WarmPalette.DarkOnTertiary,
    tertiaryContainer = WarmPalette.DarkTertiaryContainer,
    onTertiaryContainer = WarmPalette.DarkOnTertiaryContainer,
    background = WarmPalette.DarkBackground,
    onBackground = WarmPalette.DarkTextPrimary,
    surface = WarmPalette.DarkSurface,
    onSurface = WarmPalette.DarkTextPrimary,
    surfaceVariant = WarmPalette.DarkSurfaceVariant,
    onSurfaceVariant = WarmPalette.DarkTextSecondary,
    outline = WarmPalette.DarkOutline,
    outlineVariant = WarmPalette.DarkOutlineVariant,
    error = WarmPalette.DarkError,
    onError = WarmPalette.DarkOnError,
    errorContainer = WarmPalette.DarkErrorContainer,
    onErrorContainer = WarmPalette.DarkOnErrorContainer,
    surfaceTint = WarmPalette.DarkPrimary,
    inverseSurface = WarmPalette.DarkInverseSurface,
    inverseOnSurface = WarmPalette.DarkInverseOnSurface,
    inversePrimary = WarmPalette.DarkInversePrimary,
    scrim = Color.Black,
)

/**
 * 全局主题：暖色品牌色 + 大圆角 + 松排版。
 *
 * dynamicColor 默认关闭：Android 12+ 的动态取色会用壁纸色覆盖品牌色，
 * 与 `docs/design-tokens.md` 的视觉规范冲突。
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> WarmDarkColorScheme
        else -> WarmLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = WarmShapes,
        content = content,
    )
}
