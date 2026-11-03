package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    /** 信息流：未删除的记录，按时间倒序；照片随记录一起查出来。 */
    @Transaction
    @Query("SELECT * FROM posts WHERE deletedAt IS NULL ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<PostWithPhotos>>

    @Transaction
    @Query("SELECT * FROM posts WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: Long): PostWithPhotos?

    @Insert
    suspend fun insert(post: Post): Long

    @Insert
    suspend fun insertPhotos(photos: List<PostPhoto>)

    /** 记录与照片在同一个事务里写入，避免出现有记录没图的中间态。 */
    @Transaction
    suspend fun insertWithPhotos(post: Post, photos: List<PostPhoto>): Long {
        val postId = insert(post)
        insertPhotos(photos.map { it.copy(postId = postId) })
        return postId
    }

    @Query("SELECT * FROM post_photos WHERE postId = :postId ORDER BY position")
    suspend fun photosFor(postId: Long): List<PostPhoto>

    @Query("UPDATE posts SET liked = :liked WHERE id = :id")
    suspend fun setLiked(id: Long, liked: Boolean)

    /** 软删除：保留墓碑给未来的同步用，信息流靠 observeAll 过滤。 */
    @Query("UPDATE posts SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
