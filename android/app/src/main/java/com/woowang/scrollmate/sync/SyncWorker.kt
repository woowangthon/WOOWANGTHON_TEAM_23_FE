package com.woowang.scrollmate.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.woowang.scrollmate.ScrollMateApp
import java.util.concurrent.TimeUnit

private const val MAX_ATTEMPTS = 3
private const val BACKOFF_SECONDS = 30L
private const val PERIODIC_MINUTES = 15L
private const val AFTER_UPLOAD_DELAY_SECONDS = 60L
private const val WORK_NOW = "sync-questions-now"
private const val WORK_AFTER_UPLOAD = "sync-questions-after-upload"
private const val WORK_PERIODIC = "sync-questions-periodic"

// 서버에서 새 문제를 받아 기기에 저장함. 카드는 기기에 저장된 문제만 쓰므로 이 작업이 없으면 샘플 문제만 나옴
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as ScrollMateApp).container.materialRepository
        return when (SyncExecutor(repository).run()) {
            SyncOutcome.Success -> Result.success()
            SyncOutcome.Retry -> if (runAttemptCount + 1 >= MAX_ATTEMPTS) Result.failure() else Result.retry()
            is SyncOutcome.Failed -> Result.failure()
        }
    }
}

// 문제를 받아 오는 시점: 앱 시작 때, 15분마다, 자료를 올리고 1분 뒤.
// 문제는 서버에서 만들어지는 대로 쓸 수 있으므로 올린 직후에 한 번, 이후에는 주기적으로 받음
object SyncScheduler {
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun start(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.KEEP, request(delaySeconds = 0))
        workManager.enqueueUniquePeriodicWork(
            WORK_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_MINUTES, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
    }

    fun afterUpload(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_AFTER_UPLOAD,
            ExistingWorkPolicy.REPLACE,
            request(delaySeconds = AFTER_UPLOAD_DELAY_SECONDS),
        )
    }

    private fun request(delaySeconds: Long) = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
        .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
        .build()
}
