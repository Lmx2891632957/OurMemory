package com.example.myapplication.ui.feed

import com.example.myapplication.data.PostWithPhotos
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 信息流里的一个日期分组。 */
data class FeedSection(
    val dayStart: Long,
    val title: String,
    val posts: List<PostWithPhotos>,
)

/**
 * 按本地日期把记录分组：今天 / 昨天 / M月d日（跨年补年份）。
 * 输入已按时间倒序，输出保持同样的顺序。
 */
fun groupPostsByDay(
    posts: List<PostWithPhotos>,
    now: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault(),
): List<FeedSection> {
    val todayStart = startOfDay(now, timeZone)
    val yesterdayStart = Calendar.getInstance(timeZone).apply {
        timeInMillis = todayStart
        add(Calendar.DAY_OF_MONTH, -1)
    }.timeInMillis
    val currentYear = Calendar.getInstance(timeZone).apply {
        timeInMillis = todayStart
    }.get(Calendar.YEAR)

    val monthDay = SimpleDateFormat("M月d日", locale).apply { this.timeZone = timeZone }
    val fullDate = SimpleDateFormat("yyyy年M月d日", locale).apply { this.timeZone = timeZone }

    return posts.groupBy { startOfDay(it.post.createdAt, timeZone) }
        .map { (dayStart, dayPosts) ->
            val title = when (dayStart) {
                todayStart -> "今天"
                yesterdayStart -> "昨天"
                else -> {
                    val year = Calendar.getInstance(timeZone).apply {
                        timeInMillis = dayStart
                    }.get(Calendar.YEAR)
                    if (year == currentYear) {
                        monthDay.format(Date(dayStart))
                    } else {
                        fullDate.format(Date(dayStart))
                    }
                }
            }
            FeedSection(dayStart = dayStart, title = title, posts = dayPosts)
        }
}

private fun startOfDay(timestamp: Long, timeZone: TimeZone): Long =
    Calendar.getInstance(timeZone).apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
