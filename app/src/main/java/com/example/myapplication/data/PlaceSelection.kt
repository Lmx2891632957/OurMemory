package com.example.myapplication.data

/** 用户为一条记录选定的地点。label 可能来自逆地理编码，也可能是用户手填。 */
data class PlaceSelection(
    val latitude: Double,
    val longitude: Double,
    val label: String,
)

fun PlaceSelection.coordinatesText(): String =
    "%.5f, %.5f".format(latitude, longitude)
