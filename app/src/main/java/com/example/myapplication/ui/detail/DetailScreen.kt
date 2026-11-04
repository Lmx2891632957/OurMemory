package com.example.myapplication.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.PostRepository
import com.example.myapplication.data.PostWithPhotos
import com.example.myapplication.ui.components.WarmCard
import com.example.myapplication.ui.components.WarmLikeButton
import com.example.myapplication.ui.location.PlaceViewActivity
import com.example.myapplication.ui.theme.WarmPalette
import com.example.myapplication.ui.theme.WarmSizes
import com.example.myapplication.ui.theme.WarmSpacing
import com.example.myapplication.ui.util.formatDetailTime
import java.io.File

@Composable
fun DetailScreen(
    postId: Long,
    repository: PostRepository,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.factory(postId, repository),
    )
    val state = viewModel.uiState
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.message) {
        val message = viewModel.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    val content = state as? DetailUiState.Content

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DetailHeader(
                dateText = content?.let { formatDetailTime(it.item.post.createdAt) },
                showMenu = content != null,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onDeleteClick = { confirmDelete = true },
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (state) {
            DetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            DetailUiState.Missing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(WarmSpacing.xxl),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "这条记录不在了",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "可能已经被删除了",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = WarmSpacing.xs),
                        )
                    }
                }
            }

            is DetailUiState.Content -> {
                DetailContent(
                    item = state.item,
                    photoFileFor = repository::photoFile,
                    onToggleLike = viewModel::toggleLike,
                    onOpenPlace = {
                        val post = state.item.post
                        val latitude = post.latitude
                        val longitude = post.longitude
                        if (latitude != null && longitude != null) {
                            context.startActivity(
                                PlaceViewActivity.intent(
                                    context = context,
                                    latitude = latitude,
                                    longitude = longitude,
                                    label = post.placeLabel.orEmpty(),
                                ),
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("删除这条记录？") },
            text = { Text("照片也会一起删掉，不能恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete(onDeleted)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDelete = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DetailHeader(
    dateText: String?,
    showMenu: Boolean,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = WarmSpacing.pageHorizontal, vertical = WarmSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.width(WarmSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "记录详情",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = dateText ?: "只看这一刻",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        if (showMenu) {
            Box {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                ) {
                    IconButton(onClick = { onMenuOpenChange(true) }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { onMenuOpenChange(false) },
                ) {
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
                            onMenuOpenChange(false)
                            onDeleteClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    item: PostWithPhotos,
    photoFileFor: (String) -> File,
    onToggleLike: () -> Unit,
    onOpenPlace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val post = item.post
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(WarmSpacing.xs))
        PhotoPager(photos = item.photos.map { photoFileFor(it.fileName) })

        if (post.caption.isNotBlank()) {
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    horizontal = WarmSpacing.pageHorizontal,
                    vertical = WarmSpacing.md,
                ),
            )
        } else {
            Spacer(Modifier.height(WarmSpacing.sm))
        }

        if (!post.placeLabel.isNullOrBlank()) {
            val hasCoordinates = post.latitude != null && post.longitude != null
            WarmCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WarmSpacing.pageHorizontal),
                onClick = if (hasCoordinates) onOpenPlace else null,
            ) {
                Row(
                    modifier = Modifier.padding(WarmSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(WarmSpacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.placeLabel.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (post.latitude != null && post.longitude != null) {
                            Text(
                                text = "%.5f, %.5f".format(post.latitude, post.longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    if (hasCoordinates) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = "查看地图",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = WarmSpacing.xs, end = WarmSpacing.pageHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            WarmLikeButton(liked = post.liked, onToggleLike = onToggleLike)
            Text(
                text = if (post.liked) "已喜欢" else "喜欢",
                style = MaterialTheme.typography.labelLarge,
                color = if (post.liked) WarmPalette.Heart
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(WarmSpacing.xl))
    }
}

@Composable
private fun PhotoPager(photos: List<File>) {
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { photos.size })
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WarmSpacing.pageHorizontal)
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(WarmSizes.cardRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = photos[page],
                contentDescription = "第 ${page + 1} 张照片",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (photos.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(WarmSpacing.sm),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${photos.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
