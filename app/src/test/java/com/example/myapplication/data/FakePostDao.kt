package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/** 内存版 DAO，只用于 JVM 单元测试。 */
class FakePostDao : PostDao {

    val posts = MutableStateFlow<List<Post>>(emptyList())
    val photos = MutableStateFlow<List<PostPhoto>>(emptyList())

    /** 打开后下一次 insert 抛错，用来测发布失败时的回滚。 */
    var failNextInsert = false

    private var nextPostId = 1L
    private var nextPhotoId = 1L

    override fun observeAll(): Flow<List<PostWithPhotos>> =
        combine(posts, photos) { allPosts, allPhotos ->
            allPosts.filter { it.deletedAt == null }
                .sortedWith(compareByDescending<Post> { it.createdAt }.thenByDescending { it.id })
                .map { post ->
                    PostWithPhotos(
                        post = post,
                        photos = allPhotos.filter { it.postId == post.id },
                    )
                }
        }

    override suspend fun getById(id: Long): PostWithPhotos? {
        val post = posts.value.firstOrNull { it.id == id && it.deletedAt == null } ?: return null
        return PostWithPhotos(
            post = post,
            photos = photos.value.filter { it.postId == id },
        )
    }

    override fun observeById(id: Long): Flow<PostWithPhotos?> =
        combine(posts, photos) { allPosts, allPhotos ->
            allPosts.firstOrNull { it.id == id && it.deletedAt == null }?.let { post ->
                PostWithPhotos(
                    post = post,
                    photos = allPhotos.filter { it.postId == id },
                )
            }
        }

    override suspend fun insert(post: Post): Long {
        if (failNextInsert) {
            failNextInsert = false
            error("模拟数据库写入失败")
        }
        val id = nextPostId++
        posts.value = posts.value + post.copy(id = id)
        return id
    }

    override suspend fun insertPhotos(photos: List<PostPhoto>) {
        this.photos.value = this.photos.value + photos.map { it.copy(id = nextPhotoId++) }
    }

    override suspend fun photosFor(postId: Long): List<PostPhoto> =
        photos.value.filter { it.postId == postId }.sortedBy { it.position }

    override suspend fun setLiked(id: Long, liked: Boolean) {
        posts.value = posts.value.map { if (it.id == id) it.copy(liked = liked) else it }
    }

    override suspend fun softDelete(id: Long, deletedAt: Long) {
        posts.value = posts.value.map {
            if (it.id == id) it.copy(deletedAt = deletedAt, updatedAt = deletedAt) else it
        }
    }
}
