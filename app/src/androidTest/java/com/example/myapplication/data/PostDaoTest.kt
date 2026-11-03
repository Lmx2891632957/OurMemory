package com.example.myapplication.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PostDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.postDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertWithPhotos_ordersNewestFirstAndKeepsPhotoOrder() = runBlocking {
        val olderId = dao.insertWithPhotos(
            post = Post(photoPath = "old.jpg", createdAt = 1_000L),
            photos = listOf(
                PostPhoto(postId = 0, fileName = "old-1.jpg", position = 1),
                PostPhoto(postId = 0, fileName = "old-0.jpg", position = 0),
            ),
        )
        dao.insertWithPhotos(
            post = Post(photoPath = "new.jpg", createdAt = 2_000L),
            photos = listOf(PostPhoto(postId = 0, fileName = "new.jpg", position = 0)),
        )

        val posts = dao.observeAll().first()

        assertEquals(listOf("new.jpg", "old.jpg"), posts.map { it.post.photoPath })
        // @Relation 不保证顺序，展示顺序由 Repository 按 position 排
        assertEquals(setOf("old-0.jpg", "old-1.jpg"), posts.last().photos.map { it.fileName }.toSet())
        assertEquals(setOf(0, 1), posts.last().photos.map { it.position }.toSet())
        assertEquals(2, dao.getById(olderId)!!.photos.size)
    }

    @Test
    fun softDelete_hidesPostButKeepsPhotoRows() = runBlocking {
        val id = dao.insertWithPhotos(
            post = Post(photoPath = "a.jpg", createdAt = 1L),
            photos = listOf(PostPhoto(postId = 0, fileName = "a.jpg", position = 0)),
        )

        dao.softDelete(id, 5_000L)

        assertNull(dao.getById(id))
        assertEquals(0, dao.observeAll().first().size)
        // 照片行留给未来的同步墓碑，等 cleanUp 再删
        assertEquals(1, dao.photosFor(id).size)
    }

    @Test
    fun setLiked_updatesRow() = runBlocking {
        val id = dao.insertWithPhotos(
            post = Post(photoPath = "a.jpg", createdAt = 1L),
            photos = listOf(PostPhoto(postId = 0, fileName = "a.jpg", position = 0)),
        )

        dao.setLiked(id, true)

        assertTrue(dao.getById(id)!!.post.liked)
    }
}
