package com.example.myapplication.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v1 → v2 迁移测试：用导出的 1.json 建一个真实的 v1 库，写入旧格式记录，
 * 跑 [MIGRATION_1_2] 后由 Room 校验 v2 schema，并断言数据没丢、单图正确变成 post_photos 一行。
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    private fun openV1WithLegacyPosts() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO `posts` (`photoPath`, `caption`, `placeLabel`, `latitude`, `longitude`, `createdAt`, `liked`)
                VALUES ('legacy-a.jpg', '第一条记录', '天安门', 39.9087, 116.3975, 1000, 0)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO `posts` (`photoPath`, `caption`, `createdAt`, `liked`)
                VALUES ('legacy-b.jpg', '', 2000, 1)
                """.trimIndent(),
            )
            close()
        }
    }

    @Test
    fun migrate1To2_保留旧记录并补齐同步字段与照片表() = runBlocking {
        openV1WithLegacyPosts()

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        try {
            // 记录数不变
            db.query("SELECT COUNT(*) FROM posts").use { cursor ->
                cursor.moveToFirst()
                assertEquals(2, cursor.getInt(0))
            }

            // 同步字段全部有默认值，updatedAt 用 createdAt 回填
            db.query(
                """
                SELECT `id`, `photoPath`, `createdAt`, `authorId`, `deviceId`, `coordSystem`,
                       `syncState`, `updatedAt`, `remoteId`, `deletedAt`
                FROM posts ORDER BY id
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                do {
                    assertEquals("local", cursor.getString(cursor.getColumnIndexOrThrow("authorId")))
                    assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("deviceId")))
                    assertEquals("GCJ02", cursor.getString(cursor.getColumnIndexOrThrow("coordSystem")))
                    assertEquals("LOCAL_ONLY", cursor.getString(cursor.getColumnIndexOrThrow("syncState")))
                    assertEquals(
                        cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")),
                    )
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("remoteId")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("deletedAt")))
                } while (cursor.moveToNext())
            }

            // 每张旧图迁成 position=0 的一条
            db.query(
                """
                SELECT p.`photoPath`, ph.`fileName`, ph.`position`
                FROM posts p JOIN post_photos ph ON ph.`postId` = p.`id`
                ORDER BY p.`id`
                """.trimIndent(),
            ).use { cursor ->
                assertEquals(2, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("legacy-a.jpg", cursor.getString(0))
                assertEquals("legacy-a.jpg", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
                assertTrue(cursor.moveToNext())
                assertEquals("legacy-b.jpg", cursor.getString(0))
                assertEquals("legacy-b.jpg", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
            }

            // 外键级联删除：删掉记录，照片行一起消失
            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("DELETE FROM posts WHERE id = 1")
            db.query("SELECT COUNT(*) FROM post_photos").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT id FROM posts").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2L, cursor.getLong(0))
            }
            db.query("SELECT fileName FROM post_photos").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("legacy-b.jpg", cursor.getString(0))
            }
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
