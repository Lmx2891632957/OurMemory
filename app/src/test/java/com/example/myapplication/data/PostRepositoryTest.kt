package com.example.myapplication.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class PostRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var drafts: File
    private lateinit var photos: File
    private lateinit var dao: FakePostDao
    private lateinit var repository: LocalPostRepository

    @Before
    fun setUp() {
        drafts = temporaryFolder.newFolder("drafts")
        photos = temporaryFolder.newFolder("photos")
        dao = FakePostDao()
        repository = LocalPostRepository(dao, PhotoStore(drafts, photos))
    }

    private fun draftPhoto(content: String = "fake-jpeg"): File =
        File(drafts, "${UUID.randomUUID()}.jpg").apply { writeText(content) }

    @Test
    fun `create 把多张草稿转正并按顺序保存`() = runTest {
        val first = draftPhoto("1")
        val second = draftPhoto("2")
        val third = draftPhoto("3")

        val id = repository.create(
            drafts = listOf(first, second, third),
            caption = "  今天天气不错  ",
            place = PlaceSelection(39.9087, 116.3975, "天安门"),
        )

        assertFalse(first.exists())
        assertFalse(second.exists())
        assertFalse(third.exists())

        val stored = repository.observePosts().first().single()
        assertEquals(id, stored.post.id)
        assertEquals("今天天气不错", stored.post.caption)
        assertEquals("天安门", stored.post.placeLabel)
        assertEquals(39.9087, stored.post.latitude!!, 0.00001)
        assertEquals(116.3975, stored.post.longitude!!, 0.00001)

        assertEquals(3, stored.photos.size)
        assertEquals(listOf(0, 1, 2), stored.photos.map { it.position })
        assertEquals(3, stored.photos.map { it.fileName }.toSet().size)
        stored.photos.forEach { photo -> assertTrue(File(photos, photo.fileName).exists()) }
        // 旧列暂时跟着第一张照片走，等下一版表重建时删除
        assertEquals(stored.photos.first().fileName, stored.post.photoPath)
    }

    @Test
    fun `没有地点时也能发布`() = runTest {
        repository.create(listOf(draftPhoto()), "", null)

        val stored = repository.observePosts().first().single()
        assertNull(stored.post.placeLabel)
        assertNull(stored.post.latitude)
        assertEquals("", stored.post.caption)
        assertEquals(1, stored.photos.size)
    }

    @Test
    fun `没有照片时拒绝发布`() = runTest {
        val error = runCatching { repository.create(emptyList(), "空", null) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(repository.observePosts().first().isEmpty())
    }

    @Test
    fun `delete 会软删除记录并删掉所有照片文件`() = runTest {
        repository.create(listOf(draftPhoto("1"), draftPhoto("2")), "第一条", null)
        val stored = repository.observePosts().first().single()

        repository.delete(stored.post.id)

        assertTrue("软删除后不应再出现在信息流", repository.observePosts().first().isEmpty())
        val tombstone = dao.posts.value.single()
        assertNotNull(tombstone.deletedAt)
        assertEquals(tombstone.deletedAt, tombstone.updatedAt)
        stored.photos.forEach { photo -> assertFalse(File(photos, photo.fileName).exists()) }
        // 照片行保留给同步墓碑用，文件已经清理
        assertEquals(2, dao.photos.value.size)
    }

    @Test
    fun `点赞状态会被持久化`() = runTest {
        repository.create(listOf(draftPhoto()), "点亮", null)
        val stored = repository.observePosts().first().single()

        repository.setLiked(stored.post.id, true)

        assertTrue(repository.observePosts().first().single().post.liked)
    }

    @Test
    fun `observePosts 按 position 排列照片`() = runTest {
        dao.posts.value = listOf(Post(id = 1L, photoPath = "b.jpg", createdAt = 1L))
        // Room 的 @Relation 不保证顺序，这里故意按插入顺序（position 1 在前）返回
        dao.photos.value = listOf(
            PostPhoto(id = 2L, postId = 1L, fileName = "b.jpg", position = 1),
            PostPhoto(id = 1L, postId = 1L, fileName = "a.jpg", position = 0),
        )

        val stored = repository.observePosts().first().single()

        assertEquals(listOf("a.jpg", "b.jpg"), stored.photos.map { it.fileName })
    }

    @Test
    fun `observePost 返回排序后的照片且删除后为 null`() = runTest {
        repository.create(listOf(draftPhoto("1"), draftPhoto("2")), "详情", null)
        val stored = repository.observePosts().first().single()

        val loaded = repository.observePost(stored.post.id).first()
        assertNotNull(loaded)
        assertEquals(listOf(0, 1), loaded!!.photos.map { it.position })

        repository.delete(stored.post.id)

        assertNull(repository.observePost(stored.post.id).first())
    }

    @Test
    fun `入库失败时清理已经转正的照片`() = runTest {
        dao.failNextInsert = true
        val draft = draftPhoto()

        runCatching { repository.create(listOf(draft), "失败", null) }

        assertTrue(repository.observePosts().first().isEmpty())
        assertEquals(0, photos.listFiles()?.size ?: 0)
        assertFalse(draft.exists())
    }
}
