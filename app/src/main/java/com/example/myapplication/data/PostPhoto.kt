package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一条记录下的一张照片，最多 9 张，[position] 为展示顺序。
 * 删除记录时由外键级联删除；照片文件由 Repository 负责清理。
 */
@Entity(
    tableName = "post_photos",
    foreignKeys = [
        ForeignKey(
            entity = Post::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("postId")],
)
data class PostPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val fileName: String,
    val position: Int,
    val width: Int? = null,
    val height: Int? = null,
)
