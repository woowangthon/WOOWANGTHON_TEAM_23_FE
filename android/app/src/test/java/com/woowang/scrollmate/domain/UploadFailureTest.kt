package com.woowang.scrollmate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadFailureTest {
    @Test
    fun serverCodesMapToReasons() {
        assertEquals(UploadFailureReason.UNSUPPORTED_FILE, uploadFailureReason("unsupported_file"))
        assertEquals(UploadFailureReason.FILE_TOO_LARGE, uploadFailureReason("file_too_large"))
        assertEquals(UploadFailureReason.RATE_LIMITED, uploadFailureReason("rate_limited"))
    }

    @Test
    fun unknownCodeBecomesOther() {
        assertEquals(UploadFailureReason.OTHER, uploadFailureReason("invalid_request"))
        assertEquals(UploadFailureReason.OTHER, uploadFailureReason(""))
    }

    @Test
    fun failuresSurviveEncodeAndDecode() {
        val failures = listOf(
            UploadFailure(2L, listOf("스캔본.pdf", "사진.pdf"), UploadFailureReason.UNSUPPORTED_FILE),
            UploadFailure(1L, listOf("큰파일.pptx"), UploadFailureReason.FILE_TOO_LARGE),
        )

        assertEquals(failures, decodeFailures(encodeFailures(failures)))
    }

    @Test
    fun emptyListRoundTrips() {
        assertTrue(decodeFailures(encodeFailures(emptyList())).isEmpty())
    }

    @Test
    fun missingOrBrokenStoredValueGivesEmptyList() {
        assertTrue(decodeFailures(null).isEmpty())
        assertTrue(decodeFailures("").isEmpty())
        assertTrue(decodeFailures("{not json").isEmpty())
    }

    @Test
    fun unknownFieldsInStoredValueAreIgnored() {
        val stored = """[{"id":5,"fileNames":["a.pdf"],"reason":"NETWORK","extra":"무시"}]"""

        assertEquals(listOf(UploadFailure(5L, listOf("a.pdf"), UploadFailureReason.NETWORK)), decodeFailures(stored))
    }

    @Test
    fun fileNamedInServerMessageIsBlamedAndOthersAreListedSeparately() {
        val (blamed, others) = splitFailedFiles(listOf("스캔본.pdf", "정상.pdf"), "스캔본.pdf: 글자를 뽑을 수 없음 (스캔본, 이미지 슬라이드)")

        assertEquals(listOf("스캔본.pdf"), blamed)
        assertEquals(listOf("정상.pdf"), others)
    }

    @Test
    fun allFilesAreBlamedWhenMessageNamesNone() {
        val (blamed, others) = splitFailedFiles(listOf("a.pdf", "b.pdf"), "글자를 읽을 수 없는 PDF입니다")

        assertEquals(listOf("a.pdf", "b.pdf"), blamed)
        assertTrue(others.isEmpty())
    }

    @Test
    fun singleFileIsBlamedWithNoOthers() {
        val (blamed, others) = splitFailedFiles(listOf("큰파일.pptx"), "큰파일.pptx: 20MB 초과")

        assertEquals(listOf("큰파일.pptx"), blamed)
        assertTrue(others.isEmpty())
    }

    @Test
    fun failureStoredBeforeThisFieldExistedStillDecodes() {
        val stored = """[{"id":5,"fileNames":["a.pdf"],"reason":"NETWORK"}]"""

        assertTrue(decodeFailures(stored).single().alsoNotUploaded.isEmpty())
    }

    @Test
    fun alsoNotUploadedSurvivesEncodeAndDecode() {
        val failure = UploadFailure(1L, listOf("a.pdf"), UploadFailureReason.UNSUPPORTED_FILE, listOf("b.pdf"))

        assertEquals(listOf(failure), decodeFailures(encodeFailures(listOf(failure))))
    }
}
