package com.example.myapplication.ui.location

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.example.myapplication.data.location.AmapPrivacy

/** 只读地图页：显示记录的地点与坐标，不提供选点/编辑。 */
@Composable
fun PlaceViewScreen(
    latitude: Double,
    longitude: Double,
    label: String,
    onMapViewCreated: (MapView) -> Unit,
    onClose: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                AmapPrivacy.applyCompliance(context)
                MapView(context).apply {
                    onCreate(null)
                    onMapViewCreated(this)
                    map.uiSettings.apply {
                        isZoomControlsEnabled = false
                        isCompassEnabled = false
                        isScaleControlsEnabled = false
                        isRotateGesturesEnabled = false
                        isTiltGesturesEnabled = false
                    }
                    val position = LatLng(latitude, longitude)
                    map.mapType = AMap.MAP_TYPE_NORMAL
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17f))
                    map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(label.ifBlank { "记录地点" }),
                    )
                    map.setOnMapLoadedListener {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17f))
                    }
                }
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
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
                Text(
                    text = label.ifBlank { "记录地点" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "%.5f, %.5f".format(latitude, longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
