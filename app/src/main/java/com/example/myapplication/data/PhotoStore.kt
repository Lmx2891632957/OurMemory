package com.example.myapplication.data

import java.io.File
import java.util.UUID

/**
 * 照片文件管理：
 * - 拍照先写入 draftDir（cacheDir/drafts），取消编辑时可直接丢弃；
 * - 发布时移动到 photoDir（filesDir/photos），返回文件名存进数据库。
 */
class PhotoStore(
    private val draftDir: File,
    private val photoDir: File,
) {

    fun newDraftFile(): File {
        draftDir.mkdirs()
        return File(draftDir, "${UUID.randomUUID()}.jpg")
    }

    /** 把草稿转正，返回存库用的文件名。 */
    fun finalizeDraft(draft: File): String {
        photoDir.mkdirs()
        val fileName = "${UUID.randomUUID()}.jpg"
        val target = File(photoDir, fileName)
        draft.copyTo(target, overwrite = true)
        draft.delete()
        return fileName
    }

    fun fileFor(photoPath: String): File = File(photoDir, photoPath)

    fun delete(photoPath: String) {
        fileFor(photoPath).delete()
    }

    fun discardDraft(draft: File?) {
        draft?.delete()
    }

    /** 启动时清掉上次异常退出残留的草稿。 */
    fun cleanupStaleDrafts() {
        draftDir.listFiles()?.forEach { it.delete() }
    }
}
