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
    }
}

// 서버에 닿지 못한 경우 (연결 끊김, 시간 초과 등)
class NetworkException(cause: IOException) : Exception(cause.message, cause)
