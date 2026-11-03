package com.example.myapplication.ui.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.AppContainer
import com.example.myapplication.data.PlaceSelection
import com.example.myapplication.ui.components.hasPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File

/**
 * OSM 官方瓦片在部分地区（含国内网络环境）访问不稳定，这里用同样免 Key 的
 * OpenStreetMap.de 镜像；若仍加载不出来，界面会退化为"坐标 + 使用此地点"。
 */
private val OSM_DE_TILES = XYTileSource(
    "OpenStreetMap.de",
    0,
    19,
    256,
    ".png",
    arrayOf("https://tile.openstreetmap.de/"),
)

private const val DEFAULT_LAT = 39.9087
private const val DEFAULT_LNG = 116.3975

@Composable
fun LocationPickerScreen(
    initialLatitude: Double,
    initialLongitude: Double,
    container: AppContainer,
    onClose: () -> Unit,
    onPicked: (PlaceSelection) -> Unit,
) {
    val context = LocalContext.current
    val hasInitial = initialLatitude != 0.0 || initialLongitude != 0.0

    var centerLatitude by remember { mutableStateOf(if (hasInitial) initialLatitude else DEFAULT_LAT) }
    var centerLongitude by remember { mutableStateOf(if (hasInitial) initialLongitude else DEFAULT_LNG) }
    var currentLabel by remember { mutableStateOf<String?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(hasInitial) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val scope = rememberCoroutineScope()

    val recenterToCurrentLocation: () -> Unit = {
        scope.launch {
            val coordinates = runCatching { container.locationClient.currentCoordinates() }.getOrNull()
            if (coordinates != null) {
                centerLatitude = coordinates.latitude
                centerLongitude = coordinates.longitude
                mapView?.controller?.setCenter(GeoPoint(coordinates.latitude, coordinates.longitude))
                mapView?.controller?.setZoom(16.0)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) recenterToCurrentLocation() }

    LaunchedEffect(Unit) {
        if (hasInitial) return@LaunchedEffect
        val granted = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (granted) {
            container.locationClient.currentCoordinates()?.let {
                centerLatitude = it.latitude
                centerLongitude = it.longitude
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ready = true
    }

    LaunchedEffect(ready, centerLatitude, centerLongitude) {
        if (!ready) return@LaunchedEffect
        resolving = true
        delay(500)
        currentLabel = runCatching {
            container.placeResolver.labelFor(centerLatitude, centerLongitude)
        }.getOrNull() ?: "%.5f, %.5f".format(centerLatitude, centerLongitude)
        resolving = false
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (ready) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().apply {
                        userAgentValue = ctx.packageName
                        osmdroidBasePath = File(ctx.cacheDir, "osmdroid")
                        osmdroidTileCache = File(ctx.cacheDir, "osmdroid/tiles")
                    }
                    MapView(ctx).apply {
                        mapView = this
                        setTileSource(OSM_DE_TILES)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        controller.setCenter(GeoPoint(centerLatitude, centerLongitude))
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                mapCenter?.let {
                                    centerLatitude = it.latitude
                                    centerLongitude = it.longitude
                                }
                                return false
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                mapCenter?.let {
                                    centerLatitude = it.latitude
                                    centerLongitude = it.longitude
                                }
                                return false
                            }
                        })
                    }
                },
                onRelease = { it.onDetach() },
            )

            // 屏幕正中的固定图钉
            Icon(
                Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .padding(bottom = 36.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭")
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 3.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (resolving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    val coordinatesText = "%.5f, %.5f".format(centerLatitude, centerLongitude)
                    Text(
                        text = when {
                            currentLabel == null -> "拖动地图选择位置"
                            currentLabel == coordinatesText -> "选中的位置"
                            else -> currentLabel.orEmpty()
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "%.5f, %.5f".format(centerLatitude, centerLongitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val granted = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                            context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (granted) recenterToCurrentLocation() else {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }) {
                        Text("回到我的位置")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            onPicked(
                                PlaceSelection(
                                    latitude = centerLatitude,
                                    longitude = centerLongitude,
                                    label = currentLabel ?: "%.5f, %.5f".format(centerLatitude, centerLongitude),
                                ),
                            )
                        },
                    ) {
                        Text("使用此地点")
                    }
                }
            }
        }
    }
}
