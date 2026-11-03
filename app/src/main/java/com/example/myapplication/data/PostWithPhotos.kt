package com.example.myapplication.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 一条记录连同它的照片。照片表按 [PostPhoto.position] 展示，
 * Repository 会保证返回时已按 position 排序。
 */
data class PostWithPhotos(
    @Embedded val post: Post,
    @Relation(parentColumn = "id", entityColumn = "postId")
    val photos: List<PostPhoto>,
)
