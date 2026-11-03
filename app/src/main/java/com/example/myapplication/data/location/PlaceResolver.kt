package com.example.myapplication.data.location

import android.content.Context
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** 逆地理结果里的一个附近地点（POI），用于"附近地点"快捷选择。 */
data class NearbyPlace(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

data class PlaceDetails(
    val label: String?,
    val nearby: List<NearbyPlace> = emptyList(),
)

/**
 * 基于高德搜索 SDK 的逆地理编码，返回中文地址 + 附近的 POI 列表。
 * 失败（网络、Key 未授权、超时）返回空结果，由调用方退化为坐标。
 */
class PlaceResolver(private val context: Context) : PlaceLabeler {

    override suspend fun labelFor(latitude: Double, longitude: Double): String? =
        detailsFor(latitude, longitude).label

    suspend fun detailsFor(latitude: Double, longitude: Double): PlaceDetails =
        withContext(Dispatchers.Main) {
            AmapPrivacy.applyCompliance(context)
            withTimeoutOrNull(8_000L) {
                runCatching { query(latitude, longitude) }.getOrNull()
            } ?: PlaceDetails(label = null)
        }

    private suspend fun query(latitude: Double, longitude: Double): PlaceDetails =
        suspendCancellableCoroutine { continuation ->
            val search = GeocodeSearch(context)
            search.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) {
                    if (code != AMapException.CODE_AMAP_SUCCESS || result == null) {
                        if (continuation.isActive) continuation.resume(PlaceDetails(label = null))
                        return
                    }
                    val regeo = result.regeocodeAddress
                    val nearby = regeo.pois.orEmpty()
                        .mapNotNull { poi ->
                            val point = poi.latLonPoint
                            val name = poi.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            NearbyPlace(
                                name = name,
                                address = poi.snippet.orEmpty(),
                                latitude = point?.latitude ?: 0.0,
                                longitude = point?.longitude ?: 0.0,
                            )
                        }
                        .distinctBy { it.name }
                        .take(8)

                    val label = nearby.firstOrNull()?.name
                        ?: regeo.formatAddress?.takeIf { it.isNotBlank() }

                    if (continuation.isActive) {
                        continuation.resume(PlaceDetails(label = label, nearby = nearby))
                    }
                }

                override fun onGeocodeSearched(result: GeocodeResult?, code: Int) = Unit
            })

            val query = RegeocodeQuery(LatLonPoint(latitude, longitude), 200f, GeocodeSearch.AMAP)
            runCatching { search.getFromLocationAsyn(query) }
                .onFailure {
                    if (continuation.isActive) continuation.resume(PlaceDetails(label = null))
                }
        }
}
