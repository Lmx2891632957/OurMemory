package com.example.myapplication.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PostRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun draftPhoto(dir: File, content: String = "fake-jpeg"): File =
        File(dir, "draft.jpg").apply { writeText(content) }

    @Test
    fun `publish 会把草稿移动为正式照片并写入数据库`() = runTest {
        val drafts = temporaryFolder.newFolder("drafts")
        val photos = temporaryFolder.newFolder("photos")
        val dao = FakePostDao()
        val repository = PostRepository(dao, PhotoStore(drafts, photos))
        val draft = draftPhoto(drafts)

        val id = repository.publish(
            draft = draft,
            caption = "  今天天气不错  ",
            place = PlaceSelection(39.9087, 116.3975, "天安门"),
        )

        assertFalse("草稿应该被移走", draft.exists())
        val stored = repository.posts.first().single()
        assertEquals(id, stored.id)
        assertEquals("今天天气不错", stored.caption)
        assertEquals("天安门", stored.placeLabel)
        assertEquals(39.9087, stored.latitude!!, 0.00001)
        assertTrue(File(photos, stored.photoPath).exists())
    }

    @Test
    fun `没有地点时也能发布`() = runTest {
        val drafts = temporaryFolder.newFolder("drafts")
        val photos = temporaryFolder.newFolder("photos")
        val repository = PostRepository(FakePostDao(), PhotoStore(drafts, photos))

        repository.publish(draftPhoto(drafts), "", null)

        val stored = repository.posts.first().single()
        assertEquals(null, stored.placeLabel)
        assertEquals(null, stored.latitude)
        assertEquals("", stored.caption)
    }

    @Test
    fun `delete 会同时删掉记录和照片文件`() = runTest {
        val drafts = temporaryFolder.newFolder("drafts")
        val photos = temporaryFolder.newFolder("photos")
        val dao = FakePostDao()
        val repository = PostRepository(dao, PhotoStore(drafts, photos))
        repository.publish(draftPhoto(drafts), "第一条", null)
        val post = repository.posts.first().single()

        repository.delete(post)

        assertTrue(repository.posts.first().isEmpty())
        assertFalse(File(photos, post.photoPath).exists())
        assertEquals(listOf(post.id), dao.deleted.map { it.id })
    }

    @Test
    fun `点赞状态会被持久化`() = runTest {
        val drafts = temporaryFolder.newFolder("drafts")
        val photos = temporaryFolder.newFolder("photos")
        val repository = PostRepository(FakePostDao(), PhotoStore(drafts, photos))
        repository.publish(draftPhoto(drafts), "点亮", null)
        val post = repository.posts.first().single()

        repository.setLiked(post.id, true)

        assertTrue(repository.posts.first().single().liked)
    }
}
