package com.example.myapplication.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `newDraftFile 位于草稿目录且不会重名`() {
        val drafts = temporaryFolder.newFolder("drafts")
        val store = PhotoStore(drafts, temporaryFolder.newFolder("photos"))

        val first = store.newDraftFile()
        val second = store.newDraftFile()

        assertTrue(first.parentFile == drafts)
        assertNotEquals(first.name, second.name)
    }

    @Test
    fun `finalizeDraft 复制到照片目录并删除草稿`() {
        val drafts = temporaryFolder.newFolder("drafts")
        val photos = temporaryFolder.newFolder("photos")
        val store = PhotoStore(drafts, photos)
        val draft = store.newDraftFile().apply { writeText("jpeg-bytes") }

        val name = store.finalizeDraft(draft)

        assertFalse(draft.exists())
        assertTrue(store.fileFor(name).exists())
        assertEquals("jpeg-bytes", store.fileFor(name).readText())
    }

    @Test
    fun `cleanupStaleDrafts 清空草稿目录`() {
        val drafts = temporaryFolder.newFolder("drafts")
        val store = PhotoStore(drafts, temporaryFolder.newFolder("photos"))
        store.newDraftFile().writeText("stale")

        store.cleanupStaleDrafts()

        assertEquals(0, drafts.listFiles()?.size)
    }
}
