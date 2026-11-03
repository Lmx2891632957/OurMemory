package com.example.myapplication.ui.composer

import android.Manifest
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.AppContainer
import com.example.myapplication.data.PlaceSelection
import com.example.myapplication.ui.camera.CameraScreen
import com.example.myapplication.ui.components.hasPermission
import com.example.myapplication.ui.location.LocationPickerActivity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(
    photoPath: String,
    container: AppContainer,
    onClose: () -> Unit,
    onPublished: () -> Unit,
) {
    val viewModel: ComposerViewModel = viewModel(
        factory = ComposerViewModel.factory(
            photoPath = photoPath,
            repository = container.postRepository,
            locationClient = container.locationClient,
            placeResolver = container.placeResolver,
        ),
    )
    val state = viewModel.uiState
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var editingLabel by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }

    val placePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            LocationPickerActivity.parseResult(result.data)?.let(viewModel::setPlace)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.detectLocation() }

    LaunchedEffect(Unit) {
        val fine = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine || coarse) {
            viewModel.detectLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(state.published) {
        if (state.published) onPublished()
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    val discardAndClose = {
        viewModel.discardDraft()
        onClose()
    }
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestClose = {
        if (state.caption.isNotBlank() || state.place != null || state.photoPaths.size > 1) {
            confirmDiscard = true
        } else {
            discardAndClose()
        }
    }
    BackHandler(onBack = requestClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新记录") },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PhotoEditor(
                photoPaths = state.photoPaths,
                editable = !state.publishing && !state.published,
                onAdd = { showCamera = true },
                onRemove = viewModel::removePhoto,
                onMove = viewModel::movePhoto,
            )

            OutlinedTextField(
                value = state.caption,
                onValueChange = viewModel::updateCaption,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("写点什么") },
                placeholder = { Text("今天发生了什么？") },
                minLines = 3,
                maxLines = 6,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = {
                    val place = state.place
                    placePickerLauncher.launch(
                        LocationPickerActivity.intent(
                            context,
                            place?.latitude ?: 0.0,
                            place?.longitude ?: 0.0,
                        ),
                    )
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val place = state.place
                        val coordinates = place?.let { "%.5f, %.5f".format(it.latitude, it.longitude) }
                        val unresolved = place != null && place.label == coordinates
                        Text(
                            text = when {
                                state.locating -> "正在定位…"
                                place == null -> "添加地点"
                                unresolved -> "选中的位置"
                                else -> place.label
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (place != null && !state.locating && unresolved) {
                            Text(
                                text = coordinates.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (state.place != null) {
                        IconButton(onClick = { editingLabel = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "修改地点名称")
                        }
                    } else {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.detectLocation() },
                    enabled = !state.locating,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.place == null) "用当前定位" else "重新定位")
                }
            }

            Button(
                onClick = viewModel::publish,
                enabled = !state.publishing && !state.published && state.photoPaths.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.publishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("正在发布")
                } else {
                    Text("发布")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCamera) {
        Dialog(
            onDismissRequest = { showCamera = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                CameraScreen(
                    onClose = { showCamera = false },
                    onCaptured = { file ->
                        viewModel.addPhoto(file.absolutePath)
                        showCamera = false
                    },
                )
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("放弃这条记录？") },
            text = { Text("照片和文字都不会保存。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    discardAndClose()
                }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("继续编辑") }
            },
        )
    }

    if (editingLabel) {
        var text by remember { mutableStateOf(state.place?.label.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editingLabel = false },
            title = { Text("地点名称") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("比如：朝阳公园") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renamePlace(text)
                    editingLabel = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingLabel = false }) { Text("取消") }
            },
        )
    }
}

/**
 * 发布页的照片编辑区：1 张大图、2 张以上九宫格；每张可删、可左右移动，
 * 追加照片走相机弹窗，最多 [ComposerViewModel.MAX_PHOTOS] 张。
 */
@Composable
private fun PhotoEditor(
    photoPaths: List<String>,
    editable: Boolean,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            photoPaths.isEmpty() -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "还没有照片",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            photoPaths.size == 1 -> {
                PhotoTile(
                    path = photoPaths.first(),
                    index = 0,
                    count = 1,
                    editable = editable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                    onRemove = onRemove,
                    onMove = onMove,
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    photoPaths.chunked(3).forEachIndexed { rowIndex, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEachIndexed { columnIndex, path ->
                                PhotoTile(
                                    path = path,
                                    index = rowIndex * 3 + columnIndex,
                                    count = photoPaths.size,
                                    editable = editable,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    onRemove = onRemove,
                                    onMove = onMove,
                                )
                            }
                            repeat(3 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onAdd,
            enabled = editable && photoPaths.size < ComposerViewModel.MAX_PHOTOS,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    photoPaths.size >= ComposerViewModel.MAX_PHOTOS -> "最多 ${ComposerViewModel.MAX_PHOTOS} 张"
                    photoPaths.isEmpty() -> "拍一张照片"
                    else -> "再加一张（${photoPaths.size}/${ComposerViewModel.MAX_PHOTOS}）"
                },
            )
        }
    }
}

@Composable
private fun PhotoTile(
    path: String,
    index: Int,
    count: Int,
    editable: Boolean,
    modifier: Modifier,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = File(path),
            contentDescription = "第 ${index + 1} 张照片",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                .padding(horizontal = 6.dp, vertical = 1.dp),
        ) {
            Text(
                text = "${index + 1}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        if (editable) {
            IconButton(
                onClick = { onRemove(path) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(30.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "删除第 ${index + 1} 张照片",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }

            if (count > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(2.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                ) {
                    IconButton(
                        onClick = { onMove(index, -1) },
                        enabled = index > 0,
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "第 ${index + 1} 张前移",
                            tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = { onMove(index, 1) },
                        enabled = index < count - 1,
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowRight,
                            contentDescription = "第 ${index + 1} 张后移",
                            tint = if (index < count - 1) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
