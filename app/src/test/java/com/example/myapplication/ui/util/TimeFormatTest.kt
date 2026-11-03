package com.example.myapplication.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TimeFormatTest {

    private fun time(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    @Test
    fun `同一年只显示月日和时分`() {
        val createdAt = time(2026, 11, 3, 9, 5)
        val now = time(2026, 11, 5, 12, 0)

        assertEquals("11月3日 09:05", formatDetailTime(createdAt, now))
    }

    @Test
    fun `跨年补上年份`() {
        val createdAt = time(2025, 12, 31, 23, 30)
        val now = time(2026, 11, 3, 10, 0)

        assertEquals("2025年12月31日 23:30", formatDetailTime(createdAt, now))
    }
}
