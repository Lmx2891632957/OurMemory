package com.example.myapplication.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 8dp 网格与页面尺寸。页面布局优先从这里取，不写魔法数。
 * 规范见 `docs/design-tokens.md`。
 */
object WarmSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** 页面左右统一内边距。 */
    val pageHorizontal = 20.dp

    /** 卡片之间。 */
    val cardGap = 16.dp

    /** 卡片内部。 */
    val cardPadding = 16.dp

    /** 区块之间。 */
    val sectionGap = 24.dp
}

/** 组件尺寸。 */
object WarmSizes {
    val cardRadius = 20.dp
    val imageRadius = 16.dp
    val inputRadius = 16.dp
    val buttonHeight = 52.dp
    val avatar = 44.dp
    val avatarBorder = 2.dp
    val icon = 20.dp
}
