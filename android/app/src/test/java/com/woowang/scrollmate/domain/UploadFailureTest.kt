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
}
