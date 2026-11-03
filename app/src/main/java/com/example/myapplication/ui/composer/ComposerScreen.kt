package com.example.myapplication.ui.composer

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.AppContainer
import com.example.myapplication.data.PlaceSelection
import com.example.myapplication.ui.components.hasPermission
import java.io.File

const val PICKED_PLACE_KEY = "picked_place"

fun encodePickedPlace(place: PlaceSelection): String =
    "${place.latitude}|${place.longitude}|${Uri.encode(place.label)}"

fun decodePickedPlace(encoded: String): PlaceSelection? {
    val parts = encoded.split("|", limit = 3)
    if (parts.size != 3) return null
    val lat = parts[0].toDoubleOrNull() ?: return null
    val lng = parts[1].toDoubleOrNull() ?: return null
    return PlaceSelection(lat, lng, Uri.decode(parts[2]))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(
    photoPath: String,
    container: AppContainer,
    savedStateHandle: SavedStateHandle,
    onClose: () -> Unit,
    onPublished: () -> Unit,
    onPickPlace: (Double, Double) -> Unit,
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

    // 地图选点页把结果写在当前返回栈的 SavedStateHandle 里
    LaunchedEffect(savedStateHandle) {
        savedStateHandle.getStateFlow<String?>(PICKED_PLACE_KEY, null).collect { encoded ->
            if (encoded != null) {
                decodePickedPlace(encoded)?.let(viewModel::setPlace)
                savedStateHandle[PICKED_PLACE_KEY] = null
            }
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
        if (state.caption.isNotBlank() || state.place != null) {
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
            AsyncImage(
                model = File(photoPath),
                contentDescription = "刚拍的照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(16.dp)),
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
                    onPickPlace(place?.latitude ?: 0.0, place?.longitude ?: 0.0)
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
                enabled = !state.publishing && !state.published,
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
