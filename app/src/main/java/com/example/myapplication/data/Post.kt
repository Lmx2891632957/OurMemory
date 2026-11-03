package com.example.myapplication.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一条生活记录：文字 + 地点 + 时间 + 多张照片（照片在 [PostPhoto]）。
 *
 * 同步字段（remoteId/authorId/deviceId/updatedAt/deletedAt/syncState）当前只在本机写入，
 * 为以后的双人同步预留；`mood` 留给 P1。
 *
 * @property photoPath v1 的单图列。v2 起照片统一存 [PostPhoto]，这列只为兼容迁移保留，
 *   下一版确认无引用后再做表重建清理。
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
    val remoteId: String? = null,
    @ColumnInfo(defaultValue = "local")
    val authorId: String = "local",
    @ColumnInfo(defaultValue = "''")
    val deviceId: String = "",
    @ColumnInfo(defaultValue = "GCJ02")
    val coordSystem: String = "GCJ02",
    val mood: String? = null,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "LOCAL_ONLY")
    val syncState: String = "LOCAL_ONLY",
)
