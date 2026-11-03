package com.example.myapplication.ui.location

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.amap.api.maps.MapView
import com.example.myapplication.ui.theme.MyApplicationTheme

/**
 * 详情页的只读地图：只展示记录对应的坐标和地点名，不支持拖动选点。
 * 和 [LocationPickerActivity] 一样独立成 Activity，由它管理 MapView 的窗口生命周期，
 * 避免在 Compose 里销毁高德 MapView 触发 GL 线程二次 nativeDestroy。
 */
class PlaceViewActivity : ComponentActivity() {

    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()

        setContent {
            MyApplicationTheme {
                PlaceViewScreen(
                    latitude = latitude,
                    longitude = longitude,
                    label = label,
                    onMapViewCreated = { mapView = it },
                    onClose = { finish() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView?.onDestroy()
        mapView = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_LATITUDE = "extra_latitude"
        private const val EXTRA_LONGITUDE = "extra_longitude"
        private const val EXTRA_LABEL = "extra_label"

        fun intent(context: Context, latitude: Double, longitude: Double, label: String): Intent =
            Intent(context, PlaceViewActivity::class.java).apply {
                putExtra(EXTRA_LATITUDE, latitude)
                putExtra(EXTRA_LONGITUDE, longitude)
                putExtra(EXTRA_LABEL, label)
            }
    }
}
