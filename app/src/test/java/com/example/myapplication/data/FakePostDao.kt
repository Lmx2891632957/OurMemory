package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 内存版 DAO，只用于 JVM 单元测试。 */
class FakePostDao : PostDao {

    val items = MutableStateFlow<List<Post>>(emptyList())
    private var nextId = 1L
    var deleted = mutableListOf<Post>()

    override fun observeAll(): Flow<List<Post>> = items

    override suspend fun getById(id: Long): Post? = items.value.firstOrNull { it.id == id }

    override suspend fun insert(post: Post): Long {
        val id = nextId++
        items.value = (items.value + post.copy(id = id)).sortedWith(
            compareByDescending<Post> { it.createdAt }.thenByDescending { it.id },
        )
        return id
    }

    override suspend fun setLiked(id: Long, liked: Boolean) {
        items.value = items.value.map { if (it.id == id) it.copy(liked = liked) else it }
    }

    override suspend fun delete(post: Post) {
        deleted += post
        items.value = items.value.filterNot { it.id == post.id }
    }
}
