package com.woowang.scrollmate.sync

import com.woowang.scrollmate.data.MaterialRepository
import com.woowang.scrollmate.net.ApiException
import com.woowang.scrollmate.net.NetworkException

private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR_MIN = 500

sealed interface SyncOutcome {
    data object Success : SyncOutcome

    // 잠시 뒤 다시 하면 될 수 있는 경우 (네트워크 오류, 서버 오류, 429)
    data object Retry : SyncOutcome

    // 다시 해도 소용없는 경우. 다음 동기화 때 다시 시도함
    data class Failed(val code: String) : SyncOutcome
}

// 서버에서 자료 목록과 새 문제를 받아 기기에 저장함. WorkManager와 분리해 서버 응답만으로 시험할 수 있게 함
class SyncExecutor(private val repository: MaterialRepository) {
    suspend fun run(): SyncOutcome = try {
        repository.syncQuestions()
        SyncOutcome.Success
    } catch (e: NetworkException) {
        SyncOutcome.Retry
    } catch (e: ApiException) {
        if (e.httpStatus == HTTP_TOO_MANY_REQUESTS || e.httpStatus >= HTTP_SERVER_ERROR_MIN) {
            SyncOutcome.Retry
        } else {
            SyncOutcome.Failed(e.code)
        }
    }
}
