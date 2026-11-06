package com.woowang.scrollmate.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.woowang.scrollmate.domain.PDF_MIME_TYPE
import com.woowang.scrollmate.domain.PPTX_MIME_TYPE
import com.woowang.scrollmate.domain.UploadCandidate
import java.io.File
import java.util.UUID

private const val UPLOAD_CACHE_DIR = "uploads"
private const val PPTX_EXTENSION = ".pptx"

// 공유나 파일 선택으로 받은 content:// 파일을 읽는 도구.
// 공유로 받은 Uri는 받은 화면이 살아 있는 동안만 읽을 수 있어서, 올리기 전에 앱 내부로 복사함
object SharedFiles {

    fun describe(context: Context, uri: Uri): UploadCandidate {
        val resolver = context.contentResolver
        val (name, size) = nameAndSize(resolver, uri)
        return UploadCandidate(
            name = name ?: uri.lastPathSegment.orEmpty(),
            mimeType = resolver.getType(uri),
            sizeBytes = size,
        )
    }

    fun copyToCache(context: Context, uri: Uri, candidate: UploadCandidate): CachedFile? {
        val mimeType = uploadMimeType(candidate)
        val dir = File(context.cacheDir, UPLOAD_CACHE_DIR).apply { mkdirs() }
        val target = File(dir, UUID.randomUUID().toString())
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
        return CachedFile(target.absolutePath, candidate.name, mimeType)
    }

    // 공유하는 앱이 일반 형식(octet-stream)으로 보낼 수 있어서 서버에는 확장자로 판별한 형식을 보냄
    private fun uploadMimeType(candidate: UploadCandidate): String =
        if (candidate.mimeType == PPTX_MIME_TYPE || candidate.name.endsWith(PPTX_EXTENSION, ignoreCase = true)) {
            PPTX_MIME_TYPE
        } else {
            PDF_MIME_TYPE
        }

    private fun nameAndSize(resolver: ContentResolver, uri: Uri): Pair<String?, Long?> =
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null to null
            val name = cursor.getString(0)
            val size = if (cursor.isNull(1)) null else cursor.getLong(1)
            name to size
        } ?: (null to null)
}
