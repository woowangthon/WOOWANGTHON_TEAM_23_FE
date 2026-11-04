package com.woowang.scrollmate.domain

// 한 번에 올릴 수 있는 파일 수 (api.md의 POST /materials 상한)
const val MAX_UPLOAD_FILES = 5

const val PDF_MIME_TYPE = "application/pdf"
private const val PDF_EXTENSION = ".pdf"

// 올리려고 고른 파일 하나
data class UploadCandidate(
    val name: String,
    val mimeType: String?,
)

data class UploadSelection(
    val accepted: List<UploadCandidate>,
    // PDF가 아니어서 뺀 파일
    val notPdf: List<UploadCandidate>,
    // 상한을 넘어서 뺀 파일
    val overLimit: List<UploadCandidate>,
)

// 공유하는 앱에 따라 MIME 타입이 octet-stream 등으로 오기도 하므로 확장자도 함께 봄
fun isPdf(candidate: UploadCandidate): Boolean =
    candidate.mimeType == PDF_MIME_TYPE || candidate.name.endsWith(PDF_EXTENSION, ignoreCase = true)

// PDF만 남기고 상한을 넘는 파일은 뒤에서부터 뺌. 스캔본인지는 여기서 알 수 없고 서버가 415로 알려 줌
fun selectForUpload(
    candidates: List<UploadCandidate>,
    maxFiles: Int = MAX_UPLOAD_FILES,
): UploadSelection {
    val (pdfs, notPdf) = candidates.partition(::isPdf)
    return UploadSelection(
        accepted = pdfs.take(maxFiles),
        notPdf = notPdf,
        overLimit = pdfs.drop(maxFiles),
    )
}
