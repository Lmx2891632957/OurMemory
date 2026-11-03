package com.example.myapplication.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
    fun insertAndObserve_newestFirst() = runTest {
        dao.insert(Post(photoPath = "old.jpg", createdAt = 1_000L))
        dao.insert(Post(photoPath = "new.jpg", createdAt = 2_000L))

        val posts = dao.observeAll().first()

        assertEquals(listOf("new.jpg", "old.jpg"), posts.map { it.photoPath })
    }

    @Test
    fun setLiked_updatesRow() = runTest {
        val id = dao.insert(Post(photoPath = "a.jpg", createdAt = 1L))

        dao.setLiked(id, true)

        assertTrue(dao.getById(id)!!.liked)
    }

    @Test
    fun delete_removesRow() = runTest {
        val id = dao.insert(Post(photoPath = "a.jpg", createdAt = 1L))

        dao.delete(dao.getById(id)!!)

        assertNull(dao.getById(id))
        assertEquals(0, dao.observeAll().first().size)
    }
}
