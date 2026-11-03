package com.example.myapplication.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2：多图 + 同步字段。
 *
 * 用户手机上已有真实记录，这里只加列/建表、不重建 posts 表，保证数据不丢；
 * v1 的 `photoPath` 迁成 `post_photos` 里 position=0 的一张，旧列暂时保留。
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `remoteId` TEXT")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `authorId` TEXT NOT NULL DEFAULT 'local'")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `deviceId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `coordSystem` TEXT NOT NULL DEFAULT 'GCJ02'")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `mood` TEXT")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `deletedAt` INTEGER")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
        db.execSQL("UPDATE `posts` SET `updatedAt` = `createdAt` WHERE `updatedAt` = 0")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `post_photos` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `postId` INTEGER NOT NULL,
                `fileName` TEXT NOT NULL,
                `position` INTEGER NOT NULL,
                `width` INTEGER,
                `height` INTEGER,
                FOREIGN KEY(`postId`) REFERENCES `posts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_post_photos_postId` ON `post_photos` (`postId`)")
        db.execSQL(
            "INSERT INTO `post_photos` (`postId`, `fileName`, `position`) " +
                "SELECT `id`, `photoPath`, 0 FROM `posts`",
        )
    }
}
