package com.example.myapplication.data.location

/** 抽象出来方便在 JVM 单元测试里替换成假的实现。 */
interface LocationSource {
    suspend fun currentCoordinates(timeoutMs: Long = 10_000L): Coordinates?
}

interface PlaceLabeler {
    suspend fun labelFor(latitude: Double, longitude: Double): String?
}
