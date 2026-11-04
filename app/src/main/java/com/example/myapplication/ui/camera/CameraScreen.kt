package com.example.myapplication.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Size
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.components.WarmPrimaryButton
import com.example.myapplication.ui.components.hasPermission
import com.example.myapplication.ui.components.openAppSettings
import com.example.myapplication.ui.components.shouldShowRationale
import com.example.myapplication.ui.theme.WarmPalette
import com.example.myapplication.ui.theme.WarmSizes
import com.example.myapplication.ui.theme.WarmSpacing
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val NO_CAMERA = -1

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onCaptured: (File) -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasPermission(Manifest.permission.CAMERA)) }
    var askedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok ->
        granted = ok
        askedOnce = true
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!granted) {
        CameraPermissionPrompt(
            askedOnce = askedOnce,
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = { context.openAppSettings() },
            onClose = onClose,
        )
        return
    }

    CameraPreviewScreen(onClose = onClose, onCaptured = onCaptured)
}

@Composable
private fun CameraPermissionPrompt(
    askedOnce: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val cannotAskAgain = askedOnce && activity != null &&
        !activity.shouldShowRationale(Manifest.permission.CAMERA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = WarmSpacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "需要相机权限",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "记录生活要用相机拍下这一刻。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = WarmSpacing.xs, bottom = WarmSpacing.xl),
        )
        WarmPrimaryButton(
            text = if (cannotAskAgain) "去设置里打开" else "授予相机权限",
            onClick = if (cannotAskAgain) onOpenSettings else onRequest,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(WarmSpacing.xs))
        TextButton(
            onClick = onClose,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text("先不拍了")
        }
    }
}

@Composable
private fun CameraPreviewScreen(
    onClose: () -> Unit,
    onCaptured: (File) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var hasBackCamera by remember { mutableStateOf(true) }
    var hasFrontCamera by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var capturing by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1920, 1440),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            .build()
    }

    LaunchedEffect(context) {
        runCatching {
            suspendCancellableCoroutine { cont ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener(
                    {
                        try {
                            cont.resume(future.get())
                        } catch (t: Throwable) {
                            cont.resumeWithException(t)
                        }
                    },
                    ContextCompat.getMainExecutor(context),
                )
            }
        }.onSuccess { cameraProvider = it }
            .onFailure { errorMessage = "相机初始化失败：${it.message}" }
    }

    // 有的模拟器/设备只有前置或只有后置摄像头，开机时先探测一下
    LaunchedEffect(cameraProvider) {
        val provider = cameraProvider ?: return@LaunchedEffect
        hasBackCamera = runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)
        hasFrontCamera = runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
        lensFacing = when {
            hasBackCamera -> CameraSelector.LENS_FACING_BACK
            hasFrontCamera -> CameraSelector.LENS_FACING_FRONT
            else -> NO_CAMERA
        }
    }

    LaunchedEffect(previewView, cameraProvider, lensFacing) {
        val view = previewView ?: return@LaunchedEffect
        val provider = cameraProvider ?: return@LaunchedEffect
        if (lensFacing == NO_CAMERA) {
            errorMessage = "这台设备没有可用的摄像头"
            return@LaunchedEffect
        }
        runCatching {
            provider.unbindAll()
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            if (!provider.hasCamera(selector)) {
                throw IllegalStateException("这台设备没有这个方向的摄像头")
            }
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
            imageCapture.targetRotation = view.display?.rotation ?: Surface.ROTATION_0
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        }.onSuccess {
            errorMessage = null
        }.onFailure {
            errorMessage = "相机启动失败：${it.message}"
        }
    }

    val capture: () -> Unit = {
        if (!capturing) {
            capturing = true
            val file = File(context.cacheDir, "drafts/${UUID.randomUUID()}.jpg")
                .apply { parentFile?.mkdirs() }
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        capturing = false
                        onCaptured(file)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        capturing = false
                        file.delete()
                        errorMessage = "拍照失败：${exception.message}"
                    }
                },
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView = this
                }
            },
        )

        // 顶部压暗，保证暖白色返回/翻转按钮在明亮取景下也清楚
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = WarmSpacing.lg, end = WarmSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CameraCircleButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "关闭", tint = WarmPalette.TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            if (hasBackCamera && hasFrontCamera) {
                CameraCircleButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "切换摄像头",
                        tint = WarmPalette.TextPrimary,
                    )
                }
            }
        }

        if (errorMessage != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(WarmSpacing.xl),
                shape = RoundedCornerShape(WarmSizes.cardRadius),
                color = WarmPalette.Surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = WarmPalette.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(WarmSpacing.md),
                )
            }
        }

        // 底部压暗，让暖色快门在浅色画面上也清楚
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(4.dp, WarmPalette.Surface, CircleShape)
                    .padding(7.dp)
                    .background(
                        if (capturing) WarmPalette.Primary.copy(alpha = 0.55f) else WarmPalette.Primary,
                        CircleShape,
                    )
                    .semantics { contentDescription = "拍照" }
                    .clickableNoRipple { capture() },
            )
        }
    }
}

@Composable
private fun CameraCircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = WarmPalette.Surface.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
    ) {
        IconButton(onClick = onClick, content = content)
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
            indication = null,
            onClick = onClick,
        ),
    )
