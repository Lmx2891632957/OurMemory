package com.example.myapplication.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    /** 高德定位一次性返回的地址文本，可能为空 */
    val address: String? = null,
    /** 定位点所在 POI 名称（如"天安门广场"），可能为空 */
    val poiName: String? = null,
) {
    /** 优先用 POI 名，其次用地址；都没有就返回 null 让调用方走逆地理。 */
    fun displayLabel(): String? = poiName?.takeIf { it.isNotBlank() }
        ?: address?.takeIf { it.isNotBlank() }
}

/**
 * 基于高德定位 SDK 的定位实现（国内可用，不依赖 Google Play 服务）：
 * 高精度模式会混合 GPS / WiFi / 基站，一次调用同时拿到经纬度、地址和 POI 名称。
 */
class LocationClient(private val context: Context) : LocationSource {

    @SuppressLint("MissingPermission")
    override suspend fun currentCoordinates(timeoutMs: Long): Coordinates? =
        withContext(Dispatchers.Main) {
            // 高德要求：构造任何 SDK 对象前先完成隐私合规声明
            AmapPrivacy.applyCompliance(context)

            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    val client = AMapLocationClient(context)
                    var finished = false

                    fun finish(result: Coordinates?) {
                        if (finished) return
                        finished = true
                        runCatching { client.stopLocation() }
                        runCatching { client.onDestroy() }
                        if (continuation.isActive) continuation.resume(result)
                    }

                    client.setLocationListener { location ->
                        when {
                            location == null -> finish(null)
                            location.errorCode == 0 -> finish(
                                Coordinates(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    address = location.address,
                                    poiName = location.poiName,
                                ),
                            )
                            else -> finish(null)
                        }
                    }

                    val option = AMapLocationClientOption().apply {
                        locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                        isOnceLocation = true
                        isOnceLocationLatest = true
                        isNeedAddress = true
                        // 模拟器调试时允许 mock 定位
                        isMockEnable = isDebuggable()
                        httpTimeOut = timeoutMs.coerceIn(3_000L, 30_000L)
                    }
                    client.setLocationOption(option)
                    continuation.invokeOnCancellation { finish(null) }

                    runCatching { client.startLocation() }
                        .onFailure { finish(null) }
                }
            }
        }

    private fun isDebuggable(): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
