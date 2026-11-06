package com.woowang.scrollmate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadPolicyTest {
    private fun pdf(name: String, size: Long? = null) = UploadCandidate(name, PDF_MIME_TYPE, size)

    @Test
    fun pdfAndPptxByMimeTypeAreSupported() {
        assertTrue(isSupportedDocument(UploadCandidate("a", PDF_MIME_TYPE)))
        assertTrue(isSupportedDocument(UploadCandidate("a", PPTX_MIME_TYPE)))
    }

    @Test
    fun supportedByExtensionWhenMimeTypeIsGeneric() {
        assertTrue(isSupportedDocument(UploadCandidate("운영체제.PDF", "application/octet-stream")))
        assertTrue(isSupportedDocument(UploadCandidate("강의.pptx", "application/octet-stream")))
        assertTrue(isSupportedDocument(UploadCandidate("a.pdf", null)))
    }

    @Test
    fun otherFormatsAreRejected() {
        assertFalse(isSupportedDocument(UploadCandidate("a.png", "image/png")))
        assertFalse(isSupportedDocument(UploadCandidate("a.hwp", "application/x-hwp")))
        assertFalse(isSupportedDocument(UploadCandidate("a.pdf.exe", null)))
        // 구형 PPT는 지원하지 않음
        assertFalse(isSupportedDocument(UploadCandidate("a.ppt", "application/vnd.ms-powerpoint")))
    }

    @Test
    fun selectionSeparatesUnsupportedFiles() {
        val result = selectForUpload(listOf(pdf("a.pdf"), UploadCandidate("b.png", "image/png")))

        assertEquals(listOf("a.pdf"), result.accepted.map { it.name })
        assertEquals(listOf("b.png"), result.unsupported.map { it.name })
        assertTrue(result.overLimit.isEmpty())
        assertTrue(result.tooLarge.isEmpty())
    }

    @Test
    fun filesOverTheSizeLimitAreSeparated() {
        val limit = 20L * 1024 * 1024
        val files = listOf(pdf("ok.pdf", limit), pdf("big.pdf", limit + 1), pdf("unknown.pdf", null))

        val result = selectForUpload(files, maxFileBytes = limit)

        assertEquals(listOf("ok.pdf", "unknown.pdf"), result.accepted.map { it.name })
        assertEquals(listOf("big.pdf"), result.tooLarge.map { it.name })
    }

    @Test
    fun selectionDropsFilesOverTheCountLimitFromTheEnd() {
        val files = (1..7).map { pdf("$it.pdf") }

        val result = selectForUpload(files, maxFiles = 5)

        assertEquals((1..5).map { "$it.pdf" }, result.accepted.map { it.name })
        assertEquals(listOf("6.pdf", "7.pdf"), result.overLimit.map { it.name })
    }

    @Test
    fun rejectedFilesDoNotCountTowardTheCountLimit() {
        val files = (1..5).map { pdf("$it.pdf") } +
            UploadCandidate("x.png", "image/png") +
            pdf("big.pdf", Long.MAX_VALUE)

        val result = selectForUpload(files, maxFiles = 5)

        assertEquals(5, result.accepted.size)
        assertTrue(result.overLimit.isEmpty())
        assertEquals(1, result.unsupported.size)
        assertEquals(1, result.tooLarge.size)
    }

    @Test
    fun emptySelectionGivesEmptyResult() {
        val result = selectForUpload(emptyList())

        assertTrue(result.accepted.isEmpty())
        assertTrue(result.unsupported.isEmpty())
        assertTrue(result.tooLarge.isEmpty())
        assertTrue(result.overLimit.isEmpty())
    }
}
