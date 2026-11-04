package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.data.Post
import com.example.myapplication.ui.theme.WarmPalette
import com.example.myapplication.ui.theme.WarmSizes
import com.example.myapplication.ui.theme.WarmSpacing
import com.example.myapplication.ui.util.formatPostTime
import java.io.File

/**
 * 信息流卡片：暖色大圆角、头像/昵称/时间精细化排版、地点 chip、多图自适应。
 * 点击整卡进详情；点赞和删除不会冒泡到整卡。
 */
@Composable
fun PostCard(
    post: Post,
    photoFiles: List<File>,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    WarmCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = WarmSpacing.md,
                    end = WarmSpacing.xxs,
                    top = WarmSpacing.md,
                    bottom = WarmSpacing.sm,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WarmAvatar(size = 42.dp)
            Spacer(Modifier.width(WarmSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "我们",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatPostTime(post.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "更多操作",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("删除这条记录") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            confirmDelete = true
                        },
                    )
                }
            }
        }

        val placeLabel = post.placeLabel?.takeIf { it.isNotBlank() }
        if (placeLabel != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = WarmSpacing.md)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = placeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(WarmSpacing.sm))
        }

        if (photoFiles.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WarmSpacing.md)
                    .clip(RoundedCornerShape(WarmSizes.imageRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                PhotoGrid(photos = photoFiles)
            }
        }

        if (post.caption.isNotBlank()) {
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    horizontal = WarmSpacing.md,
                    vertical = WarmSpacing.sm,
                ),
            )
        } else {
            Spacer(Modifier.height(WarmSpacing.xs))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = WarmSpacing.xs, end = WarmSpacing.md, bottom = WarmSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            WarmLikeButton(liked = post.liked, onToggleLike = onToggleLike)
            Text(
                text = if (post.liked) "已喜欢" else "喜欢",
                style = MaterialTheme.typography.labelLarge,
                color = if (post.liked) WarmPalette.Heart else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (photoFiles.size > 1) {
                Text(
                    text = "${photoFiles.size} 张照片",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = WarmSpacing.xxs),
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条记录？") },
            text = { Text("照片也会一起删掉，不能恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }
}

/**
 * 卡片照片排版：1 张 4:3 大图，2 张并排，4 张 2×2，其余 3 列九宫格。
 * 外圈由卡片统一裁 16dp 圆角，内部留 2dp 缝。
 */
@Composable
private fun PhotoGrid(photos: List<File>) {
    if (photos.isEmpty()) return

    if (photos.size == 1) {
        AsyncImage(
            model = photos.first(),
            contentDescription = "记录照片",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
        )
        return
    }

    val columns = if (photos.size == 2 || photos.size == 4) 2 else 3
    Column(modifier = Modifier.fillMaxWidth()) {
        photos.chunked(columns).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { photo ->
                    AsyncImage(
                        model = photo,
                        contentDescription = "记录照片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    )
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
