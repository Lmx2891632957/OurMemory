package com.example.myapplication.ui.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.PostRepository
import com.example.myapplication.ui.components.PostCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
        topBar = {
            TopAppBar(title = { Text("我们的记录") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPhoto) {
                Icon(Icons.Filled.Add, contentDescription = "记录一张照片")
            }
        },
    ) { innerPadding ->
        if (posts.isEmpty()) {
            EmptyFeed(modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else {
            LazyColumn(
                // 顶部内边距放在 modifier 上：让列表视口从顶栏下方开始，
                // 否则吸顶的分组头会贴到 y=0，被 TopAppBar 盖住
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { section ->
                    stickyHeader(key = "day-${section.dayStart}") {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                        )
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
private fun EmptyFeed(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "还没有记录",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "点右下角的 + ，拍下今天的第一张照片",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
