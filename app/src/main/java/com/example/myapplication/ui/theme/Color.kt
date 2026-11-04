package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 暖色品牌色：Design Tokens 的单一来源。
 *
 * 页面优先使用 `MaterialTheme.colorScheme.*`；只有红心、星星这类品牌点缀
 * 才直接用这里的颜色。不要在页面里新增 `Color(0xFF...)`。
 *
 * 规范说明见 `docs/design-tokens.md`。
 */
object WarmPalette {

    // ---- 浅色（燕麦奶白） ----
    val Background = Color(0xFFFAF7F2)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF4EDE4)

    val Primary = Color(0xFFE0A96D)
    val OnPrimary = Color(0xFF3C2A1B)
    val PrimaryInk = Color(0xFFA96332)
    val PrimaryContainer = Color(0xFFF6E4D1)
    val OnPrimaryContainer = Color(0xFF5A3B23)

    val Secondary = Color(0xFFC9A98A)
    val OnSecondary = Color(0xFF3C2A1B)
    val SecondaryContainer = Color(0xFFF1E4D5)
    val OnSecondaryContainer = Color(0xFF53402E)

    val Tertiary = Color(0xFFE8A598)
    val OnTertiary = Color(0xFF4A2620)
    val TertiaryContainer = Color(0xFFFAE3DD)
    val OnTertiaryContainer = Color(0xFF5C3029)

    val TextPrimary = Color(0xFF3C3633)
    val TextSecondary = Color(0xFF8C827B)
    val Outline = Color(0xFFEFE7DC)
    val OutlineVariant = Color(0xFFF7F1E9)

    val Heart = Color(0xFFE06C6C)
    val Star = Color(0xFFF2C14E)
    val AvatarBorder = Color(0xFFF0D6BA)

    val Error = Color(0xFFC0564A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFF9E3DF)
    val OnErrorContainer = Color(0xFF5C1F1A)

    val InverseSurface = Color(0xFF3C3633)
    val InverseOnSurface = Color(0xFFFAF7F2)
    val InversePrimary = Color(0xFFF0C79D)

    // ---- 深色（暖褐夜色，不用纯黑） ----
    val DarkBackground = Color(0xFF1E1A17)
    val DarkSurface = Color(0xFF29231F)
    val DarkSurfaceVariant = Color(0xFF352D27)

    val DarkPrimary = Color(0xFFE8B07A)
    val DarkOnPrimary = Color(0xFF3A291B)
    val DarkPrimaryContainer = Color(0xFF4A3525)
    val DarkOnPrimaryContainer = Color(0xFFF6DFC9)

    val DarkSecondary = Color(0xFFC9A98A)
    val DarkOnSecondary = Color(0xFF2E2118)
    val DarkSecondaryContainer = Color(0xFF43352A)
    val DarkOnSecondaryContainer = Color(0xFFE8D7C6)

    val DarkTertiary = Color(0xFFE8A598)
    val DarkOnTertiary = Color(0xFF3A1F1A)
    val DarkTertiaryContainer = Color(0xFF563029)
    val DarkOnTertiaryContainer = Color(0xFFF8DAD4)

    val DarkTextPrimary = Color(0xFFF3EAE1)
    val DarkTextSecondary = Color(0xFFB9ACA0)
    val DarkOutline = Color(0xFF4D4239)
    val DarkOutlineVariant = Color(0xFF3B322B)

    val DarkHeart = Color(0xFFF08A8A)
    val DarkStar = Color(0xFFF2C14E)
    val DarkAvatarBorder = Color(0xFF5A4330)

    val DarkError = Color(0xFFF0A79E)
    val DarkOnError = Color(0xFF3B0E0A)
    val DarkErrorContainer = Color(0xFF5A2420)
    val DarkOnErrorContainer = Color(0xFFF8DAD4)

    val DarkInverseSurface = Color(0xFFF3EAE1)
    val DarkInverseOnSurface = Color(0xFF1E1A17)
    val DarkInversePrimary = Color(0xFFA96332)
}
