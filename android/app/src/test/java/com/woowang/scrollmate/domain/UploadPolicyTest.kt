package com.woowang.scrollmate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadPolicyTest {
    private fun pdf(name: String) = UploadCandidate(name, PDF_MIME_TYPE)

    @Test
    fun pdfByMimeTypeIsAccepted() {
        assertTrue(isPdf(UploadCandidate("a", PDF_MIME_TYPE)))
    }

    @Test
    fun pdfByExtensionIsAcceptedWhenMimeTypeIsGeneric() {
        assertTrue(isPdf(UploadCandidate("운영체제.PDF", "application/octet-stream")))
        assertTrue(isPdf(UploadCandidate("a.pdf", null)))
    }

    @Test
    fun nonPdfIsRejected() {
        assertFalse(isPdf(UploadCandidate("a.png", "image/png")))
        assertFalse(isPdf(UploadCandidate("a.pdf.exe", null)))
    }

    @Test
    fun selectionSeparatesNonPdfFiles() {
        val result = selectForUpload(listOf(pdf("a.pdf"), UploadCandidate("b.png", "image/png")))

        assertEquals(listOf("a.pdf"), result.accepted.map { it.name })
        assertEquals(listOf("b.png"), result.notPdf.map { it.name })
        assertTrue(result.overLimit.isEmpty())
    }

    @Test
    fun selectionDropsFilesOverTheLimitFromTheEnd() {
        val files = (1..7).map { pdf("$it.pdf") }

        val result = selectForUpload(files, maxFiles = 5)

        assertEquals((1..5).map { "$it.pdf" }, result.accepted.map { it.name })
        assertEquals(listOf("6.pdf", "7.pdf"), result.overLimit.map { it.name })
    }

    @Test
    fun nonPdfFilesDoNotCountTowardTheLimit() {
        val files = (1..5).map { pdf("$it.pdf") } + UploadCandidate("x.png", "image/png")

        val result = selectForUpload(files, maxFiles = 5)

        assertEquals(5, result.accepted.size)
        assertTrue(result.overLimit.isEmpty())
        assertEquals(1, result.notPdf.size)
    }

    @Test
    fun emptySelectionGivesEmptyResult() {
        val result = selectForUpload(emptyList())

        assertTrue(result.accepted.isEmpty())
        assertTrue(result.notPdf.isEmpty())
        assertTrue(result.overLimit.isEmpty())
    }
}
