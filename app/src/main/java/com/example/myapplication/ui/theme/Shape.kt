package com.example.myapplication.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 大圆角、无攻击性的形状系统。 */
val WarmShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(WarmSizes.imageRadius),
    large = RoundedCornerShape(WarmSizes.cardRadius),
    extraLarge = RoundedCornerShape(28.dp),
)
