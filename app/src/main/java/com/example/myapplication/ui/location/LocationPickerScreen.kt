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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.example.myapplication.AppContainer
import com.example.myapplication.data.PlaceSelection
import com.example.myapplication.data.location.AmapPrivacy
import com.example.myapplication.data.location.NearbyPlace
import com.example.myapplication.ui.components.hasPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val hasInitial = initialLatitude != 0.0 || initialLongitude != 0.0

    var center by remember {
        mutableStateOf(
            if (hasInitial) LatLng(initialLatitude, initialLongitude) else LatLng(DEFAULT_LAT, DEFAULT_LNG),
        )
    }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentLabel by remember { mutableStateOf<String?>(null) }
    var nearby by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var resolving by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(hasInitial) }

    val coordinatesText = "%.5f, %.5f".format(center.latitude, center.longitude)

    fun moveTo(latLng: LatLng) {
        center = latLng
        mapView?.map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
    }

    val recenterToCurrentLocation: () -> Unit = {
        scope.launch {
            val coordinates = runCatching { container.locationClient.currentCoordinates() }.getOrNull()
            if (coordinates != null) {
                moveTo(LatLng(coordinates.latitude, coordinates.longitude))
                ready = true
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        ready = true
        if (granted) recenterToCurrentLocation()
    }

    LaunchedEffect(Unit) {
        if (hasInitial) return@LaunchedEffect
        val granted = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (granted) {
            container.locationClient.currentCoordinates()?.let {
                center = LatLng(it.latitude, it.longitude)
                mapView?.map?.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 17f))
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ready = true
    }

    // 地图停止移动后做逆地理，拿中文地址 + 附近 POI
    LaunchedEffect(center.latitude, center.longitude) {
        resolving = true
        delay(400)
        val details = runCatching {
            container.placeResolver.detailsFor(center.latitude, center.longitude)
        }.getOrNull()
        currentLabel = details?.label
        nearby = details?.nearby.orEmpty()
        resolving = false
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onPause()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                AmapPrivacy.applyCompliance(ctx)
                MapView(ctx).apply {
                    onCreate(null)
                    onResume()
                    map.uiSettings.apply {
                        isZoomControlsEnabled = false
                        isCompassEnabled = false
                        isScaleControlsEnabled = false
                        isRotateGesturesEnabled = false
                        isTiltGesturesEnabled = false
                    }
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 17f))
                    map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                        override fun onCameraChange(position: CameraPosition?) = Unit

                        override fun onCameraChangeFinish(position: CameraPosition?) {
                            position?.target?.let { target ->
                                if (target.latitude != center.latitude || target.longitude != center.longitude) {
                                    center = target
                                }
                            }
                        }
                    })
                    mapView = this
                }
            },
            onRelease = { it.onDestroy() },
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭")
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ) {
                TextButton(onClick = {
                    val granted = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (granted) recenterToCurrentLocation() else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) {
                    Text("回到我的位置")
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
                    Text(
                        text = when {
                            !ready -> "正在定位…"
                            currentLabel.isNullOrBlank() -> "选中的位置"
                            else -> currentLabel.orEmpty()
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = coordinatesText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (nearby.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "附近地点",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(nearby, key = { it.name }) { place ->
                            FilterChip(
                                selected = place.name == currentLabel,
                                onClick = {
                                    currentLabel = place.name
                                    if (place.latitude != 0.0 && place.longitude != 0.0) {
                                        moveTo(LatLng(place.latitude, place.longitude))
                                    }
                                },
                                label = { Text(place.name) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            onPicked(
                                PlaceSelection(
                                    latitude = center.latitude,
                                    longitude = center.longitude,
                                    label = currentLabel?.takeIf { it.isNotBlank() } ?: coordinatesText,
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
