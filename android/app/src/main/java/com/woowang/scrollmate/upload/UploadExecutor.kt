package com.woowang.scrollmate.upload

import com.woowang.scrollmate.data.MaterialRepository
import com.woowang.scrollmate.net.ApiException
import com.woowang.scrollmate.net.NetworkException
import com.woowang.scrollmate.net.UploadFile

private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR_MIN = 500

sealed interface UploadOutcome {
    data object Success : UploadOutcome

    // 잠시 뒤 다시 시도하면 될 수 있는 경우 (네트워크 오류, 서버 오류, 동시 처리 한도)
    data object Retry : UploadOutcome

    // 다시 해도 소용없는 경우. 파일을 바꾸거나 한도가 풀릴 때까지 기다려야 함. code는 api.md의 오류 코드.
    // message는 서버가 보낸 설명이며 문제가 된 파일 이름이 들어 있을 수 있음
    data class Failed(val code: String, val message: String = "") : UploadOutcome
}

// 업로드 결과를 재시도 여부로 분류함. WorkManager와 분리해 서버 응답만으로 시험할 수 있게 함
class UploadExecutor(private val repository: MaterialRepository) {
    suspend fun run(files: List<UploadFile>, subjectId: Long?): UploadOutcome = try {
        repository.upload(files, subjectId)
        UploadOutcome.Success
    } catch (e: NetworkException) {
        UploadOutcome.Retry
    } catch (e: ApiException) {
        classify(e)
    }

    private fun classify(e: ApiException): UploadOutcome = when {
        // 하루 한도는 몇 번 다시 해도 풀리지 않으므로 바로 실패로 둠
        e.code == ApiException.CODE_RATE_LIMITED -> UploadOutcome.Failed(e.code, e.message)
        e.httpStatus == HTTP_TOO_MANY_REQUESTS || e.httpStatus >= HTTP_SERVER_ERROR_MIN -> UploadOutcome.Retry
        else -> UploadOutcome.Failed(e.code, e.message)
    }
}
