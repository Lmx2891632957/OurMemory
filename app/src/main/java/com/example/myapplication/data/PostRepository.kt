package com.example.myapplication.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 记录仓库。UI 只依赖这个接口；以后接同步时换成远程/本地混合实现，页面不用改。
 * 本期不提供编辑已发布记录的能力。
 */
interface PostRepository {

    /** 信息流数据源：已过滤软删除，照片按 position 排好序。 */
    fun observePosts(): Flow<List<PostWithPhotos>>

    /**
     * 把草稿照片转正并写入一条记录，最多 9 张。
     * @return 新记录的 id
     */
    suspend fun create(
        drafts: List<File>,
        caption: String,
        place: PlaceSelection?,
        mood: String? = null,
    ): Long

    /** 单张照片的旧入口，等价于 [create] 一张草稿。 */
    suspend fun publish(draft: File, caption: String, place: PlaceSelection?): Long =
        create(listOf(draft), caption, place)

    suspend fun setLiked(id: Long, liked: Boolean)

    /** 软删除记录，并删除它在本机的所有照片文件。 */
    suspend fun delete(id: Long)

    fun photoFile(fileName: String): File
}

class LocalPostRepository(
    private val dao: PostDao,
    private val photoStore: PhotoStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PostRepository {

    override fun observePosts(): Flow<List<PostWithPhotos>> =
        dao.observeAll().map { items ->
            items.map { item -> item.copy(photos = item.photos.sortedBy(PostPhoto::position)) }
        }

    override suspend fun create(
        drafts: List<File>,
        caption: String,
        place: PlaceSelection?,
        mood: String?,
    ): Long {
        require(drafts.isNotEmpty()) { "至少需要一张照片" }
        val fileNames = withContext(ioDispatcher) { drafts.map(photoStore::finalizeDraft) }
        val now = System.currentTimeMillis()
        val post = Post(
            // 旧列暂时保留：v1 的读取路径和迁移回滚时还能用，下一版表重建时删掉
            photoPath = fileNames.first(),
            caption = caption.trim(),
            placeLabel = place?.label,
            latitude = place?.latitude,
            longitude = place?.longitude,
            createdAt = now,
            updatedAt = now,
            mood = mood,
        )
        return try {
            dao.insertWithPhotos(
                post = post,
                photos = fileNames.mapIndexed { index, fileName ->
                    PostPhoto(postId = 0, fileName = fileName, position = index)
                },
            )
        } catch (t: Throwable) {
            // 数据库写失败时把已经落盘的照片清掉，避免留下孤儿文件
            withContext(ioDispatcher) { fileNames.forEach(photoStore::delete) }
            throw t
        }
    }

    override suspend fun setLiked(id: Long, liked: Boolean) = dao.setLiked(id, liked)

    override suspend fun delete(id: Long) {
        val photos = dao.photosFor(id)
        dao.softDelete(id, System.currentTimeMillis())
        withContext(ioDispatcher) { photos.forEach { photoStore.delete(it.fileName) } }
    }

    override fun photoFile(fileName: String): File = photoStore.fileFor(fileName)
}
