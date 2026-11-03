package com.example.myapplication.ui.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** "刚刚 / 12 分钟前 / 3 小时前 / 11月3日 19:20" */
fun formatPostTime(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - createdAt).coerceAtLeast(0L)
    return when {
        diff < DateUtils.MINUTE_IN_MILLIS -> "刚刚"
        diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS} 分钟前"
        diff < DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.HOUR_IN_MILLIS} 小时前"
        else -> SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(createdAt))
    }
}

/** 详情页标题："11月3日 10:24"，不是今年的补上年份。 */
fun formatDetailTime(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val postYear = Calendar.getInstance().apply { timeInMillis = createdAt }.get(Calendar.YEAR)
    val currentYear = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
    val pattern = if (postYear == currentYear) "M月d日 HH:mm" else "yyyy年M月d日 HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(createdAt))
}
