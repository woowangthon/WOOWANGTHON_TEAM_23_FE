package com.woowang.scrollmate.upload

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.woowang.scrollmate.ScrollMateApp
import com.woowang.scrollmate.domain.UploadFailureReason
import com.woowang.scrollmate.domain.splitFailedFiles
import com.woowang.scrollmate.domain.uploadFailureReason
import com.woowang.scrollmate.net.UploadFile
import com.woowang.scrollmate.sync.SyncScheduler
import java.io.File
import java.util.concurrent.TimeUnit

private const val KEY_PATHS = "paths"
private const val KEY_NAMES = "names"
private const val KEY_MIME_TYPES = "mime_types"
private const val KEY_SUBJECT_ID = "subject_id"
private const val NO_SUBJECT = -1L
private const val MAX_ATTEMPTS = 3
private const val BACKOFF_SECONDS = 30L

// 앱 내부 저장소로 복사해 둔 업로드 대기 파일
data class CachedFile(val path: String, val name: String, val mimeType: String)

// 화면이 닫혀도 업로드가 이어지도록 WorkManager에서 올림
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val paths = inputData.getStringArray(KEY_PATHS) ?: return Result.failure()
        val names = inputData.getStringArray(KEY_NAMES) ?: return Result.failure()
        val mimeTypes = inputData.getStringArray(KEY_MIME_TYPES) ?: return Result.failure()
        val subjectId = inputData.getLong(KEY_SUBJECT_ID, NO_SUBJECT).takeIf { it != NO_SUBJECT }

        val files = paths.indices.map { i ->
            UploadFile(names[i], mimeTypes[i]) { File(paths[i]).inputStream() }
        }
        val container = (applicationContext as ScrollMateApp).container
        val repository = container.materialRepository

        return when (val outcome = UploadExecutor(repository).run(files, subjectId)) {
            UploadOutcome.Success -> {
                deleteCached(paths)
                SyncScheduler.afterUpload(applicationContext)
                Result.success()
            }
            UploadOutcome.Retry ->
                if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                    deleteCached(paths)
                    container.uploadFailures.record(names.toList(), UploadFailureReason.NETWORK)
                    Result.failure()
                } else {
                    Result.retry()
                }
            is UploadOutcome.Failed -> {
                deleteCached(paths)
                val reason = uploadFailureReason(outcome.code)
                // 파일 자체가 문제인 경우에만 어느 파일인지 나눠 알려 줌
                val fileProblem = reason == UploadFailureReason.UNSUPPORTED_FILE || reason == UploadFailureReason.FILE_TOO_LARGE
                val (blamed, others) =
                    if (fileProblem) splitFailedFiles(names.toList(), outcome.message) else names.toList() to emptyList()
                container.uploadFailures.record(blamed, reason, others)
                Result.failure()
            }
        }
    }

    private fun deleteCached(paths: Array<String>) = paths.forEach { File(it).delete() }
}

object UploadScheduler {
    fun enqueue(context: Context, files: List<CachedFile>, subjectId: Long?) {
        val input = Data.Builder()
            .putStringArray(KEY_PATHS, files.map { it.path }.toTypedArray())
            .putStringArray(KEY_NAMES, files.map { it.name }.toTypedArray())
            .putStringArray(KEY_MIME_TYPES, files.map { it.mimeType }.toTypedArray())
            .putLong(KEY_SUBJECT_ID, subjectId ?: NO_SUBJECT)
            .build()
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
