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

    // 다시 해도 소용없는 경우. 파일을 바꿔야 함 (스캔본, 용량 초과 등). code는 api.md의 오류 코드
    data class Failed(val code: String) : UploadOutcome
}

// 업로드 결과를 재시도 여부로 분류함. WorkManager와 분리해 서버 응답만으로 시험할 수 있게 함
class UploadExecutor(private val repository: MaterialRepository) {
    suspend fun run(files: List<UploadFile>, subjectId: Long?): UploadOutcome = try {
        repository.upload(files, subjectId)
        UploadOutcome.Success
    } catch (e: NetworkException) {
        UploadOutcome.Retry
    } catch (e: ApiException) {
        if (e.httpStatus == HTTP_TOO_MANY_REQUESTS || e.httpStatus >= HTTP_SERVER_ERROR_MIN) {
            UploadOutcome.Retry
        } else {
            UploadOutcome.Failed(e.code)
        }
    }
}
