package com.example.myapplication.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 经纬度 → 可读地址。
 *
 * 先用系统 Geocoder；如果设备上没有可用的地理编码后端（模拟器、部分国产 ROM、断网），
 * 再退到免 Key 的 Photon（OSM）接口。两者都失败就返回 null，由调用方展示坐标。
 */
class PlaceResolver(private val context: Context) : PlaceLabeler {

    override suspend fun labelFor(latitude: Double, longitude: Double): String? {
        val fromSystem = withTimeoutOrNull(6_000L) {
            runCatching { systemAddress(latitude, longitude) }.getOrNull()
        }?.let { readableName(it) }
        if (fromSystem != null) return fromSystem

        return withTimeoutOrNull(8_000L) {
            runCatching { photonLabel(latitude, longitude) }.getOrNull()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun systemAddress(latitude: Double, longitude: Double): Address? =
        withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addressList: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addressList)
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(emptyList())
                            }
                        },
                    )
                }
            } else {
                geocoder.getFromLocation(latitude, longitude, 1).orEmpty()
            }
            addresses.firstOrNull()
        }

    private fun readableName(address: Address): String? {
        val parts = listOfNotNull(
            address.thoroughfare,
            address.subLocality?.takeIf { it != address.thoroughfare },
            address.locality?.takeIf { it != address.subLocality },
        ).map { it.trim() }.filter { it.isNotEmpty() }

        if (parts.isNotEmpty()) return parts.distinct().joinToString(" ")

        return address.getAddressLine(0)?.takeIf { it.isNotBlank() }
    }

    private suspend fun photonLabel(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            val body = runCatching {
                val url = URL("https://photon.komoot.io/reverse?lat=$latitude&lon=$longitude&limit=1")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    setRequestProperty("User-Agent", "OurMoments/1.0 (Android)")
                }
                try {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull() ?: return@withContext null

            runCatching {
                val features = JSONObject(body).optJSONArray("features") ?: return@runCatching null
                if (features.length() == 0) return@runCatching null
                val properties = features.getJSONObject(0).getJSONObject("properties")
                val name = properties.optString("name").takeIf { it.isNotBlank() }
                val street = properties.optString("street").takeIf { it.isNotBlank() }
                val houseNumber = properties.optString("housenumber").takeIf { it.isNotBlank() }
                val locality = properties.optString("locality").takeIf { it.isNotBlank() }
                val district = properties.optString("district").takeIf { it.isNotBlank() }
                val city = properties.optString("city").takeIf { it.isNotBlank() }

                name
                    ?: listOfNotNull(street, houseNumber).joinToString(" ").takeIf { it.isNotBlank() }
                    ?: locality
                    ?: district
                    ?: city
            }.getOrNull()
        }
}
