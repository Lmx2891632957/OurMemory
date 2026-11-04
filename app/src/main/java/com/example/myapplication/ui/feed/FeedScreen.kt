package com.example.myapplication.ui.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.PostRepository
import com.example.myapplication.ui.components.PostCard
import com.example.myapplication.ui.components.WarmDayHeader
import com.example.myapplication.ui.theme.WarmPalette
import com.example.myapplication.ui.theme.WarmSpacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    repository: PostRepository,
    onAddPhoto: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val viewModel: FeedViewModel = viewModel(factory = FeedViewModel.factory(repository))
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val sections = remember(posts) { groupPostsByDay(posts) }

    // 新记录插到列表最前面时，如果用户本来就在顶部附近，就把视图滚回顶部，
    // 否则 LazyColumn 会按 key 锚定旧记录，导致新记录的头部被顶出屏幕。
    LaunchedEffect(posts.firstOrNull()?.post?.id) {
        val newestId = posts.firstOrNull()?.post?.id
        if (newestId != null && listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { FeedHeader() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPhoto,
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                ),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "记录一条",
                    modifier = Modifier.size(28.dp),
                )
            }
        },
    ) { innerPadding ->
        if (posts.isEmpty()) {
            EmptyFeed(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                // 顶部内边距放在 modifier 上：让列表视口从顶栏下方开始，
                // 否则吸顶的分组头会贴到 y=0，被顶栏盖住
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                state = listState,
                contentPadding = PaddingValues(
                    start = WarmSpacing.pageHorizontal,
                    end = WarmSpacing.pageHorizontal,
                    top = WarmSpacing.xxs,
                    bottom = innerPadding.calculateBottomPadding() + 112.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.cardGap),
            ) {
                sections.forEach { section ->
                    stickyHeader(key = "day-${section.dayStart}") {
                        WarmDayHeader(text = section.title)
                    }
                    items(section.posts, key = { "post-${it.post.id}" }) { item ->
                        PostCard(
                            post = item.post,
                            photoFiles = item.photos.map { repository.photoFile(it.fileName) },
                            onClick = { onOpenDetail(item.post.id) },
                            onToggleLike = { viewModel.toggleLike(item) },
                            onDelete = { viewModel.delete(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = WarmSpacing.pageHorizontal)
            .padding(top = WarmSpacing.lg, bottom = WarmSpacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "我们的记录",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "把每一天，轻轻收好。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = WarmPalette.Heart,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyFeed(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = WarmSpacing.xxl),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = WarmPalette.Heart,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(WarmSpacing.xl))
            Text(
                text = "还没有记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(WarmSpacing.xs))
            Text(
                text = "点右下角的 +，拍下今天的第一张照片",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
