package com.woowang.scrollmate.net

import java.io.IOException

// 서버가 오류 응답을 돌려준 경우. code는 api.md의 오류 코드 (invalid_request, unsupported_file 등)
class ApiException(
    val httpStatus: Int,
    val code: String,
    override val message: String,
) : Exception(message) {
    companion object {
        const val CODE_UNKNOWN = "unknown"

        // 동시에 처리 중인 자료가 너무 많음. 잠시 뒤 다시 하면 됨 (429)
        const val CODE_TOO_MANY_MATERIALS = "too_many_materials"

        // 기기 ID별 하루 사용 한도 초과 (429). 다시 시도해도 당분간 같은 결과임
        const val CODE_RATE_LIMITED = "rate_limited"
    }
}

// 서버에 닿지 못한 경우 (연결 끊김, 시간 초과 등)
class NetworkException(cause: IOException) : Exception(cause.message, cause)
