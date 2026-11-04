package com.example.myapplication.ui.composer

import android.Manifest
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.AppContainer
import com.example.myapplication.data.PlaceSelection
import com.example.myapplication.ui.camera.CameraScreen
import com.example.myapplication.ui.components.WarmPrimaryButton
import com.example.myapplication.ui.components.hasPermission
import com.example.myapplication.ui.location.LocationPickerActivity
import com.example.myapplication.ui.theme.WarmPalette
import com.example.myapplication.ui.theme.WarmSizes
import com.example.myapplication.ui.theme.WarmSpacing
import java.io.File

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ComposerHeader(onClose = requestClose) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = WarmSpacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.xl),
        ) {
            PhotoSection(
                photoPaths = state.photoPaths,
                editable = !state.publishing && !state.published,
                onAdd = { showCamera = true },
                onRemove = viewModel::removePhoto,
                onMove = viewModel::movePhoto,
            )

            CaptionSection(
                caption = state.caption,
                onCaptionChange = viewModel::updateCaption,
            )

            LocationSection(
                locating = state.locating,
                place = state.place,
                onPickPlace = {
                    val place = state.place
                    placePickerLauncher.launch(
                        LocationPickerActivity.intent(
                            context,
                            place?.latitude ?: 0.0,
                            place?.longitude ?: 0.0,
                        ),
                    )
                },
                onEditLabel = { editingLabel = true },
                onDetectLocation = { viewModel.detectLocation() },
            )

            Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
                WarmPrimaryButton(
                    text = "发布",
                    onClick = viewModel::publish,
                    enabled = !state.publishing && !state.published && state.photoPaths.isNotEmpty(),
                    loading = state.publishing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "照片和文字只保存在这台手机",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(WarmSpacing.xl))
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("放弃这条记录？") },
            text = { Text("照片和文字都不会保存。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        discardAndClose()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("放弃") }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDiscard = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("继续编辑") }
            },
        )
    }

    if (editingLabel) {
        var text by remember { mutableStateOf(state.place?.label.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editingLabel = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("地点名称") },
            text = {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("比如：朝阳公园") },
                    shape = RoundedCornerShape(WarmSizes.inputRadius),
                    colors = warmTextFieldColors(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renamePlace(text)
                        editingLabel = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingLabel = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ComposerHeader(onClose: () -> Unit) {
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
            IconButton(onClick = onClose) {
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
                text = "新记录",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "留住这一刻",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

@Composable
private fun PhotoSection(
    photoPaths: List<String>,
    editable: Boolean,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
        SectionLabel(
            title = "照片",
            trailing = "${photoPaths.size}/${ComposerViewModel.MAX_PHOTOS}",
        )

        if (photoPaths.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
                shape = RoundedCornerShape(WarmSizes.cardRadius),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "还没有照片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            EditorPhotoGrid(
                photoPaths = photoPaths,
                editable = editable,
                onRemove = onRemove,
                onMove = onMove,
            )
        }

        OutlinedButton(
            onClick = onAdd,
            enabled = editable && photoPaths.size < ComposerViewModel.MAX_PHOTOS,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
            border = BorderStroke(
                width = 1.dp,
                color = if (editable && photoPaths.size < ComposerViewModel.MAX_PHOTOS) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline
                },
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = WarmPalette.PrimaryInk,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = when {
                    photoPaths.size >= ComposerViewModel.MAX_PHOTOS -> "最多 ${ComposerViewModel.MAX_PHOTOS} 张"
                    photoPaths.isEmpty() -> "拍一张照片"
                    else -> "再加一张（${photoPaths.size}/${ComposerViewModel.MAX_PHOTOS}）"
                },
            )
        }
    }
}

@Composable
private fun CaptionSection(
    caption: String,
    onCaptionChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
        SectionLabel(title = "写点什么", trailing = "可选")
        TextField(
            value = caption,
            onValueChange = onCaptionChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "今天发生了什么？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(WarmSizes.inputRadius),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = warmTextFieldColors(),
        )
    }
}

@Composable
private fun LocationSection(
    locating: Boolean,
    place: PlaceSelection?,
    onPickPlace: () -> Unit,
    onEditLabel: () -> Unit,
    onDetectLocation: () -> Unit,
) {
    val coordinates = place?.let { "%.5f, %.5f".format(it.latitude, it.longitude) }
    val unresolved = place != null && place.label == coordinates

    Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs)) {
        SectionLabel(title = "地点", trailing = "可手动修改")

        Surface(
            onClick = onPickPlace,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(WarmSizes.cardRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
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
                    if (locating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(WarmSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            locating -> "正在定位…"
                            place == null -> "添加地点"
                            unresolved -> "选中的位置"
                            else -> place.label
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = when {
                            locating -> "正在获取当前位置"
                            place == null -> "让记录带上这一刻在哪里"
                            else -> coordinates.orEmpty()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (place != null) {
                    IconButton(onClick = onEditLabel) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "修改地点名称",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        TextButton(
            onClick = onDetectLocation,
            enabled = !locating,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (place == null) "用当前定位" else "重新定位")
        }
    }
}

@Composable
private fun SectionLabel(
    title: String,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun warmTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = WarmPalette.PrimaryInk,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun EditorPhotoGrid(
    photoPaths: List<String>,
    editable: Boolean,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    if (photoPaths.size == 1) {
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
        return
    }

    val columns = if (photoPaths.size == 2 || photoPaths.size == 4) 2 else 3
    Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs)) {
        photoPaths.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(WarmSpacing.xs)) {
                row.forEachIndexed { columnIndex, path ->
                    PhotoTile(
                        path = path,
                        index = rowIndex * columns + columnIndex,
                        count = photoPaths.size,
                        editable = editable,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onRemove = onRemove,
                        onMove = onMove,
                    )
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
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
            .clip(RoundedCornerShape(WarmSizes.imageRadius))
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
