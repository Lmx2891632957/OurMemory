package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一条生活记录：照片 + 文字 + 地点 + 时间。
 * photoPath 只存文件名（位于 filesDir/photos 下），避免内部路径变化后失效。
 */
@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoPath: String,
    val caption: String = "",
    val placeLabel: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long,
    val liked: Boolean = false,
)
