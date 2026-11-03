package com.example.myapplication.ui.feed

import com.example.myapplication.data.Post
import com.example.myapplication.data.PostPhoto
import com.example.myapplication.data.PostWithPhotos
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class FeedGroupingTest {

    private val zone: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")
    private val locale: Locale = Locale.CHINA

    private fun time(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private fun post(id: Long, createdAt: Long): PostWithPhotos = PostWithPhotos(
        post = Post(id = id, photoPath = "$id.jpg", createdAt = createdAt),
        photos = listOf(PostPhoto(id = id, postId = id, fileName = "$id.jpg", position = 0)),
    )

    @Test
    fun `按今天 昨天 日期分组并保持时间倒序`() {
        val now = time(2026, 11, 3, 10)
        val posts = listOf(
            post(4, time(2026, 11, 3, 9)),
            post(3, time(2026, 11, 2, 23)),
            post(2, time(2026, 10, 31, 20)),
            post(1, time(2025, 12, 31, 8)),
        )

        val sections = groupPostsByDay(posts, now, zone, locale)

        assertEquals(listOf("今天", "昨天", "10月31日", "2025年12月31日"), sections.map { it.title })
        assertEquals(
            listOf(listOf(4L), listOf(3L), listOf(2L), listOf(1L)),
            sections.map { section -> section.posts.map { it.post.id } },
        )
    }

    @Test
    fun `同一天的记录合并成一组`() {
        val now = time(2026, 11, 3, 10)
        val posts = listOf(
            post(2, time(2026, 11, 3, 9)),
            post(1, time(2026, 11, 3, 0, 1)),
        )

        val sections = groupPostsByDay(posts, now, zone, locale)

        assertEquals(1, sections.size)
        assertEquals("今天", sections.single().title)
        assertEquals(2, sections.single().posts.size)
    }

    @Test
    fun `午夜前后分到不同日期`() {
        val now = time(2026, 11, 3, 10)
        val posts = listOf(
            post(2, time(2026, 11, 3, 0, 1)),
            post(1, time(2026, 11, 2, 23, 59)),
        )

        val sections = groupPostsByDay(posts, now, zone, locale)

        assertEquals(listOf("今天", "昨天"), sections.map { it.title })
    }
}
