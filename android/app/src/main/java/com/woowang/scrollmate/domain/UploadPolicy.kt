package com.woowang.scrollmate.domain

// 한 번에 올릴 수 있는 파일 수와 파일당 용량 (BE 이슈 #4에서 정한 상한)
const val MAX_UPLOAD_FILES = 5
const val MAX_UPLOAD_FILE_BYTES = 20L * 1024 * 1024

const val PDF_MIME_TYPE = "application/pdf"
const val PPTX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation"

// 올릴 수 있는 형식. HWP는 지원하지 않음
val SUPPORTED_MIME_TYPES = arrayOf(PDF_MIME_TYPE, PPTX_MIME_TYPE)
private val SUPPORTED_EXTENSIONS = listOf(".pdf", ".pptx")

// 올리려고 고른 파일 하나. 용량을 알 수 없으면 sizeBytes는 null
data class UploadCandidate(
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long? = null,
)

data class UploadSelection(
    val accepted: List<UploadCandidate>,
    // PDF나 PPTX가 아니어서 뺀 파일
    val unsupported: List<UploadCandidate>,
    // 파일당 용량 상한을 넘어서 뺀 파일
    val tooLarge: List<UploadCandidate>,
    // 개수 상한을 넘어서 뺀 파일
    val overLimit: List<UploadCandidate>,
)

// 공유하는 앱에 따라 MIME 타입이 octet-stream 등으로 오기도 하므로 확장자도 함께 봄
fun isSupportedDocument(candidate: UploadCandidate): Boolean =
    candidate.mimeType in SUPPORTED_MIME_TYPES ||
        SUPPORTED_EXTENSIONS.any { candidate.name.endsWith(it, ignoreCase = true) }

// 지원하는 형식만 남기고, 용량과 개수 상한을 넘는 파일을 뺌.
// 글자를 뽑을 수 없는 스캔본인지와 쪽수 상한은 여기서 알 수 없고 서버가 415, 413으로 알려 줌
fun selectForUpload(
    candidates: List<UploadCandidate>,
    maxFiles: Int = MAX_UPLOAD_FILES,
    maxFileBytes: Long = MAX_UPLOAD_FILE_BYTES,
): UploadSelection {
    val (supported, unsupported) = candidates.partition(::isSupportedDocument)
    val (tooLarge, withinSize) = supported.partition { (it.sizeBytes ?: 0L) > maxFileBytes }
    return UploadSelection(
        accepted = withinSize.take(maxFiles),
        unsupported = unsupported,
        tooLarge = tooLarge,
        overLimit = withinSize.drop(maxFiles),
    )
}
