package com.example.myapplication.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class Coordinates(val latitude: Double, val longitude: Double)

/**
 * 取"当前位置"，按可用性依次尝试：
 * 1. Google Play 服务的 FusedLocationProvider（最省电、最准）；
 * 2. 系统 LocationManager 的最后已知位置；
 * 3. 系统 LocationManager 的一次实时定位。
 *
 * 国产手机很多没有 GMS，第 1 步会直接失败/抛异常，所以必须有 2、3 兜底。
 * 全部拿不到就返回 null，UI 退化为手动在地图上选点。
 */
class LocationClient(private val context: Context) : LocationSource {

    // GMS 不存在时 getFusedLocationProviderClient 会抛异常，这里提前吃掉
    private val fusedClient by lazy {
        runCatching { LocationServices.getFusedLocationProviderClient(context) }.getOrNull()
    }

    override suspend fun currentCoordinates(timeoutMs: Long): Coordinates? {
        val fromPlayServices = fusedClient?.let { client ->
            runCatching { queryFused(client, timeoutMs) }.getOrNull()
        }
        return fromPlayServices ?: platformLocation(timeoutMs)
    }

    @SuppressLint("MissingPermission")
    private suspend fun queryFused(
        client: com.google.android.gms.location.FusedLocationProviderClient,
        timeoutMs: Long,
    ): Coordinates? {
        val fresh = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Coordinates?> { cont ->
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (cont.isActive) {
                            cont.resume(location?.let { Coordinates(it.latitude, it.longitude) })
                        }
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        }
        if (fresh != null) return fresh

        return withTimeoutOrNull(2_000L) {
            suspendCancellableCoroutine<Coordinates?> { cont ->
                client.lastLocation
                    .addOnSuccessListener { location ->
                        if (cont.isActive) {
                            cont.resume(location?.let { Coordinates(it.latitude, it.longitude) })
                        }
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun platformLocation(timeoutMs: Long): Coordinates? =
        withContext(Dispatchers.IO) {
            val manager = runCatching {
                context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            }.getOrNull() ?: return@withContext null

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            ).filter { provider ->
                runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
            }

            // 先看最后已知位置，通常瞬间就能拿到
            for (provider in providers) {
                val last: Location? = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
                if (last != null) return@withContext Coordinates(last.latitude, last.longitude)
            }

            if (providers.isEmpty()) return@withContext null

            // 再等一次实时定位
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Coordinates?> { cont ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            runCatching { manager.removeUpdates(this) }
                            if (cont.isActive) {
                                cont.resume(Coordinates(location.latitude, location.longitude))
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                        override fun onProviderEnabled(provider: String) = Unit

                        override fun onProviderDisabled(provider: String) = Unit
                    }
                    cont.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
                    runCatching {
                        val provider = providers.first()
                        manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                    }.onFailure {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        }
}
