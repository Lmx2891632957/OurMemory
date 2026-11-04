# 设计规范（Design Tokens）

> 版本：v1（2026-11-04）· 配套 `docs/prd.md`、`docs/ui-flows.md`
> 原则：**温暖、治愈、可爱、有呼吸感。** 所有页面只从本文档和 `ui/theme` 取值，不在页面里临时调色。

## 1. 色彩

代码入口：`ui/theme/Color.kt` 的 `WarmPalette`，并映射到 Material 3 `ColorScheme`。
页面优先使用 `MaterialTheme.colorScheme.*`；只有品牌点缀（红心、星星）直接用 `WarmPalette.*`。

### 浅色（默认）

| Token | 值 | Material 3 角色 | 用途 |
|---|---|---|---|
| `Background` | `#FAF7F2` | `background` | 燕麦奶白页面底色 |
| `Surface` | `#FFFFFF` | `surface` | 卡片、输入框 |
| `SurfaceVariant` | `#F4EDE4` | `surfaceVariant` | 次级卡片、地点行 |
| `Primary` | `#E0A96D` | `primary` | 按钮填充、选中态、暖色点缀 |
| `OnPrimary` | `#3C2A1B` | `onPrimary` | 主色上的文字（深可可，**不用白字**） |
| `PrimaryContainer` | `#F6E4D1` | `primaryContainer` | 头像底、轻强调块 |
| `OnPrimaryContainer` | `#5A3B23` | `onPrimaryContainer` | 容器上的文字 |
| `PrimaryInk` | `#A96332` | — | 需要对比度的图标/文字链接 |
| `TextPrimary` | `#3C3633` | `onSurface` / `onBackground` | 标题、正文 |
| `TextSecondary` | `#8C827B` | `onSurfaceVariant` | 时间、辅助说明 |
| `Outline` | `#EFE7DC` | `outline` | 极淡分隔线（尽量不用硬边框） |
| `OutlineVariant` | `#F7F1E9` | `outlineVariant` | 更淡的分隔 |
| `Heart` | `#E06C6C` | — | 点赞红心 |
| `Star` | `#F2C14E` | — | 收藏/小星星 |
| `Error` | `#C0564A` | `error` | 删除、错误提示 |
| `AvatarBorder` | `#F0D6BA` | — | 头像细边框 |

### 深色（暖褐夜色）

| Token | 值 | 用途 |
|---|---|---|
| `DarkBackground` | `#1E1A17` | 页面底色 |
| `DarkSurface` | `#29231F` | 卡片 |
| `DarkSurfaceVariant` | `#352D27` | 次级卡片 |
| `DarkPrimary` | `#E8B07A` | 主色 |
| `DarkOnPrimary` | `#3A291B` | 主色上的文字 |
| `DarkPrimaryContainer` | `#4A3525` | 头像底 |
| `DarkOnPrimaryContainer` | `#F6DFC9` | 容器文字 |
| `DarkTextPrimary` | `#F3EAE1` | 标题、正文 |
| `DarkTextSecondary` | `#B9ACA0` | 辅助文字 |
| `DarkOutline` | `#4D4239` | 分隔线 |
| `DarkHeart` | `#F08A8A` | 深色下的红心 |

**硬性规则**

- 不用纯黑 `#000000`，不用纯白大面积铺底（卡片除外）。
- 主色 `#E0A96D` 上只能放深可可字；白字对比度只有约 2.1:1。
- 需要长按/点击识别的文字链接用 `PrimaryInk`，不用 `Primary`。
- 关闭 Android 12+ 动态取色，否则系统会覆盖品牌色。

## 2. 字体

系统默认中文字体（体积 0，中文显示稳定）。标题用 `SemiBold`，正文 `Normal`，避免 700 以上的粗黑。

| 样式 | 字号 / 行高 | 字重 | 用途 |
|---|---|---|---|
| `titleLarge` | 22sp / 30sp | SemiBold | 页面主标题 |
| `titleMedium` | 17sp / 24sp | SemiBold | 区块标题 |
| `titleSmall` | 16sp / 22sp | SemiBold | 卡片标题、昵称 |
| `bodyLarge` | 16sp / 26sp | Normal | 详情正文 |
| `bodyMedium` | 15sp / 24sp | Normal | 卡片正文 |
| `bodySmall` | 13sp / 20sp | Normal | 坐标、辅助说明 |
| `labelLarge` | 14sp / 20sp | Medium | 按钮、日期分组头 |
| `labelMedium` | 12sp / 18sp | Medium | 图片计数、标签 |
| `labelSmall` | 11sp / 16sp | Medium | 极小说明 |

> 以后若引入「得意黑」等圆体，只用于标题，并注意字体授权与 APK 体积。

## 3. 形状与圆角

| Token | 值 | 用途 |
|---|---|---|
| `WarmShapes.extraSmall` | 8dp | 小标签 |
| `WarmShapes.small` | 12dp | 缩略图、小控件 |
| `WarmShapes.medium` | 16dp | 输入框、图片 |
| `WarmShapes.large` | 20dp | 卡片 |
| `WarmShapes.extraLarge` | 28dp | 大面板 |
| 按钮 | 全圆角 Pill | 主按钮 |
| 头像 | 圆形 | 所有头像 |

## 4. 间距（8dp 网格）

代码入口：`ui/theme/Dimens.kt`。

| Token | 值 | 用途 |
|---|---|---|
| `WarmSpacing.xxs` | 4dp | 图标与文字 |
| `WarmSpacing.xs` | 8dp | 紧凑间距 |
| `WarmSpacing.sm` | 12dp | 卡片内小分块 |
| `WarmSpacing.md` | 16dp | 卡片内边距、卡片间距 |
| `WarmSpacing.lg` | 20dp | 页面左右边距 |
| `WarmSpacing.xl` | 24dp | 区块间距 |
| `WarmSpacing.xxl` | 32dp | 大区块/空态 |

- 页面左右统一 **20dp**（比旧版 16dp 更有呼吸感）。
- 信息流卡片之间 **16dp**；日期分组头与卡片之间由分组头内边距承担。
- 卡片内边距 **16–18dp**，内容不要贴边。

## 5. 阴影

- 卡片：白底 + `shadowElevation = 4dp`，`tonalElevation = 0dp`，**无边框**。
- 浮动按钮/底部面板：6–8dp。
- 不用硬边框分隔，优先用 `Outline` 淡线或留白。
- Android 的 elevation 阴影偏紧，无法完全复刻网页的大面积弥散投影；用「米白底 + 白卡 + 大圆角 + 4dp 阴影」近似，保证列表性能。

## 6. 交互与细节

- 点击反馈：暖色低透明度 ripple + 轻微缩放（0.97–0.985），使用 `WarmCard` / `warmPressScale`。
- 点赞：红心 + 轻微弹跳（`WarmLikeButton`）。
- 头像：40–44dp 圆形，2dp 暖色细边框。
- 卡片整卡可点，内部按钮/菜单不触发整卡点击。
- 反馈文案用 Snackbar，不使用打断式弹窗（删除确认除外）。

## 7. 共享组件

代码入口：`ui/components/WarmComponents.kt`。

```kotlin
// 卡片
WarmCard(onClick = { openDetail() }) {
    Text("今天去吃了热干面")
}

// 主按钮（自带 loading）
WarmPrimaryButton(
    text = "发布",
    onClick = viewModel::publish,
    loading = state.publishing,
    modifier = Modifier.fillMaxWidth(),
)

// 头像
WarmAvatar(label = "我")

// 点赞
WarmLikeButton(liked = post.liked, onToggleLike = viewModel::toggleLike)

// 日期分组头
WarmDayHeader("今天")
```

| 组件 | 说明 |
|---|---|
| `WarmCard` | 白底 20dp 圆角、4dp 阴影、可选点击与按压缩放 |
| `WarmPrimaryButton` | 52dp 高、Pill、暖色底深可可字、支持 loading |
| `WarmAvatar` | 圆形、暖色细边框、默认「我」 |
| `WarmLikeButton` | 红心/空心心切换 + 弹跳 |
| `WarmDayHeader` | 日期分组头，吸顶时保持不透明背景 |
| `Modifier.warmPressScale` | 给自定义可点击元素加温和缩放 |

## 8. 落地状态

- 本版本已实现 token 层、共享组件与深色暖色方案，并全局关闭动态取色。
- **页面结构未改**；现有页面的颜色会随主题自动变化。
- 旧页面仍有少量硬编码颜色（信息流点赞红、相机黑底、图片占位灰），在后续逐页迁移时替换。
- 新页面必须直接使用本文档的 token 与共享组件，不再新增硬编码色值。
