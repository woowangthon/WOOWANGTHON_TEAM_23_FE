package com.woowang.scrollmate.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 올리지 못한 이유. 화면에서 이유와 다음에 할 일을 알려 주는 데 씀
enum class UploadFailureReason {
    // 글자를 읽을 수 없는 파일 (스캔본 PDF, 글자 없는 슬라이드만 있는 PPTX) (415)
    UNSUPPORTED_FILE,

    // 파일이 너무 큼. 파일당 20MB 또는 100쪽(슬라이드) 초과 (413)
    FILE_TOO_LARGE,

    // 오늘 올릴 수 있는 한도를 넘음 (429 rate_limited)
    RATE_LIMITED,

    // 네트워크나 서버 오류로 다시 시도해도 올리지 못함
    NETWORK,

    OTHER,
}

// 서버 오류 코드(api.md)를 이유로 바꿈
fun uploadFailureReason(code: String): UploadFailureReason = when (code) {
    "unsupported_file" -> UploadFailureReason.UNSUPPORTED_FILE
    "file_too_large" -> UploadFailureReason.FILE_TOO_LARGE
    "rate_limited" -> UploadFailureReason.RATE_LIMITED
    else -> UploadFailureReason.OTHER
}

// 올리지 못한 업로드 한 건. 앱 안 저장소에 기록해 두었다가 화면에 보여 줌
@Serializable
data class UploadFailure(
    val id: Long,
    // 이유에 해당하는 파일. 어느 파일인지 알 수 없으면 함께 올린 파일 전부
    val fileNames: List<String>,
    val reason: UploadFailureReason,
    // 문제는 없었지만 같은 요청에 있어서 함께 올라가지 못한 파일
    val alsoNotUploaded: List<String> = emptyList(),
)

// 서버는 여러 파일 중 하나만 문제여도 요청 전체를 거절하고, 설명에 문제가 된 파일 이름을 넣음.
// 설명에 이름이 나온 파일과 나머지를 나눔. 이름을 찾지 못하면 전부를 문제 파일로 봄
fun splitFailedFiles(allNames: List<String>, serverMessage: String): Pair<List<String>, List<String>> {
    val (blamed, others) = allNames.partition { it.isNotBlank() && serverMessage.contains(it) }
    return if (blamed.isEmpty()) allNames to emptyList() else blamed to others
}

private val json = Json { ignoreUnknownKeys = true }

fun encodeFailures(failures: List<UploadFailure>): String = json.encodeToString(failures)

// 저장된 값이 없거나 깨졌으면 빈 목록. 안내가 사라질 뿐 앱이 멈추지는 않게 함
fun decodeFailures(text: String?): List<UploadFailure> {
    if (text.isNullOrBlank()) return emptyList()
    return runCatching { json.decodeFromString<List<UploadFailure>>(text) }.getOrDefault(emptyList())
}
