package com.woowang.scrollmate.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.woowang.scrollmate.domain.UploadCandidate
import java.io.File
import java.util.UUID

private const val UPLOAD_CACHE_DIR = "uploads"
private const val CACHE_EXTENSION = ".pdf"

// 공유나 파일 선택으로 받은 content:// 파일을 읽는 도구.
// 공유로 받은 Uri는 받은 화면이 살아 있는 동안만 읽을 수 있어서, 올리기 전에 앱 내부로 복사함
object SharedFiles {

    fun describe(context: Context, uri: Uri): UploadCandidate {
        val resolver = context.contentResolver
        return UploadCandidate(
            name = displayName(resolver, uri) ?: uri.lastPathSegment.orEmpty(),
            mimeType = resolver.getType(uri),
        )
    }

    fun copyToCache(context: Context, uri: Uri, name: String): CachedFile? {
        val dir = File(context.cacheDir, UPLOAD_CACHE_DIR).apply { mkdirs() }
        val target = File(dir, UUID.randomUUID().toString() + CACHE_EXTENSION)
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
        return CachedFile(target.absolutePath, name)
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
