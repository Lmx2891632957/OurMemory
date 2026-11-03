package com.example.myapplication.ui.location

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.amap.api.maps.MapView
import com.example.myapplication.MyApplication
import com.example.myapplication.data.PlaceSelection
import com.example.myapplication.ui.theme.MyApplicationTheme

/**
 * 地图选点页。
 *
 * 单独做成 Activity 而不是 Compose 导航页，是因为高德 MapView 的 onResume/onPause/onDestroy
 * 必须和窗口生命周期严格对齐：在 Compose 里销毁 MapView 会和 View 脱离窗口时高德内部的
 * 销毁撞车，GL 线程二次执行 nativeDestroy 直接 SIGABRT。放在 Activity 里就是官方支持的用法。
 */
class LocationPickerActivity : ComponentActivity() {

    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialLatitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        val initialLongitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
        val container = (application as MyApplication).container

        setContent {
            MyApplicationTheme {
                LocationPickerScreen(
                    initialLatitude = initialLatitude,
                    initialLongitude = initialLongitude,
                    container = container,
                    onMapViewCreated = { mapView = it },
                    onClose = { finish() },
                    onPicked = { place ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().apply {
                                putExtra(EXTRA_RESULT_LATITUDE, place.latitude)
                                putExtra(EXTRA_RESULT_LONGITUDE, place.longitude)
                                putExtra(EXTRA_RESULT_LABEL, place.label)
                            },
                        )
                        finish()
                    },
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
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_RESULT_LATITUDE = "extra_result_latitude"
        const val EXTRA_RESULT_LONGITUDE = "extra_result_longitude"
        const val EXTRA_RESULT_LABEL = "extra_result_label"

        fun intent(context: Context, latitude: Double, longitude: Double): Intent =
            Intent(context, LocationPickerActivity::class.java).apply {
                putExtra(EXTRA_LATITUDE, latitude)
                putExtra(EXTRA_LONGITUDE, longitude)
            }

        fun parseResult(intent: Intent?): PlaceSelection? {
            intent ?: return null
            if (!intent.hasExtra(EXTRA_RESULT_LATITUDE)) return null
            return PlaceSelection(
                latitude = intent.getDoubleExtra(EXTRA_RESULT_LATITUDE, 0.0),
                longitude = intent.getDoubleExtra(EXTRA_RESULT_LONGITUDE, 0.0),
                label = intent.getStringExtra(EXTRA_RESULT_LABEL).orEmpty(),
            )
        }
    }
}
