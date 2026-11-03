package com.example.myapplication.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class PostRepository(
    private val dao: PostDao,
    private val photoStore: PhotoStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    val posts: Flow<List<Post>> = dao.observeAll()

    /**
     * 把草稿照片转正并写入数据库。
     * @return 新记录的 id
     */
    suspend fun publish(draft: File, caption: String, place: PlaceSelection?): Long {
        val fileName = withContext(ioDispatcher) { photoStore.finalizeDraft(draft) }
        return dao.insert(
            Post(
                photoPath = fileName,
                caption = caption.trim(),
                placeLabel = place?.label,
                latitude = place?.latitude,
                longitude = place?.longitude,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setLiked(id: Long, liked: Boolean) = dao.setLiked(id, liked)

    suspend fun delete(post: Post) {
        dao.delete(post)
        withContext(ioDispatcher) { photoStore.delete(post.photoPath) }
    }

    fun photoFile(post: Post): File = photoStore.fileFor(post.photoPath)
}
