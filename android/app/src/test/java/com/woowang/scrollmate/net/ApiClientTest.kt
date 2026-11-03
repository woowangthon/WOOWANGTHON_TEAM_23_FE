package com.woowang.scrollmate.net

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class ApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ApiClient(baseUrl = server.url("/").toString(), deviceId = { DEVICE_ID })
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun fixture(name: String): String =
        javaClass.getResource("/fixtures/$name")!!.readText(Charsets.UTF_8)

    private fun enqueue(code: Int, body: String? = null) {
        server.enqueue(MockResponse().setResponseCode(code).apply { if (body != null) setBody(body) })
    }

    private fun pdf(name: String) = UploadFile(name, "application/pdf") {
        ByteArrayInputStream("%PDF-1.4 sample".toByteArray())
    }

    @Test
    fun uploadSendsMultipartFilesAndDeviceId() = runBlocking {
        enqueue(202, fixture("upload_accepted.json"))

        val result = client.uploadMaterials(listOf(pdf("운영체제.pdf"), pdf("데이터베이스.pdf")))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/materials", request.path)
        assertEquals(DEVICE_ID, request.getHeader("X-Device-Id"))
        assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
        val body = request.body.readUtf8()
        assertEquals(2, Regex("name=\"files\"").findAll(body).count())
        assertTrue(body.contains("filename="))
        assertEquals(listOf("m_010", "m_011"), result.map { it.id })
        assertEquals("processing", result.first().status)
    }

    @Test
    fun listMaterialsParsesProgress() = runBlocking {
        enqueue(200, fixture("materials_list.json"))

        val result = client.listMaterials()

        assertEquals("GET", server.takeRequest().method)
        assertEquals(2, result.size)
        assertEquals(4, result[0].chunksDone)
        assertEquals(12, result[0].chunksTotal)
        assertEquals("ready", result[1].status)
        assertEquals(14, result[1].questionCount)
    }

    @Test
    fun deleteMaterialHandlesNoContent() = runBlocking {
        enqueue(204)

        client.deleteMaterial("m_001")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/materials/m_001", request.path)
    }

    @Test
    fun getQuestionsParsesOptionalFields() = runBlocking {
        enqueue(200, fixture("questions.json"))

        val result = client.getQuestions()

        assertEquals("/api/v1/questions", server.takeRequest().path)
        assertEquals("2026-11-03T21:30:00+09:00", result.serverTime)
        val ox = result.questions[0]
        assertEquals("ox", ox.type)
        assertEquals(3, ox.page)
        assertTrue(ox.accepted.isEmpty())
        val short = result.questions[1]
        assertEquals(listOf("페이징", "paging"), short.accepted)
        assertNull(short.page)
    }

    @Test
    fun getQuestionsSendsSinceQuery() = runBlocking {
        enqueue(200, fixture("questions.json"))

        client.getQuestions(since = "2026-11-03T21:30:00+09:00")

        val path = server.takeRequest().path!!
        assertTrue(path.startsWith("/api/v1/questions?since="))
    }

    @Test
    fun postAnswersSendsJsonBody() = runBlocking {
        enqueue(200, """{ "recorded": 1 }""")

        val recorded = client.postAnswers(
            listOf(AnswerDto("q_001", "wrong", "2026-11-03T21:30:00+09:00")),
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/answers", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"questionId\":\"q_001\""))
        assertTrue(body.contains("\"result\":\"wrong\""))
        assertEquals(1, recorded)
    }

    @Test
    fun errorResponsesMapToApiException() {
        val cases = listOf(
            Triple(415, "error_unsupported_file.json", "unsupported_file"),
            Triple(413, "error_file_too_large.json", "file_too_large"),
            Triple(429, "error_too_many_materials.json", "too_many_materials"),
            Triple(502, "error_llm_unavailable.json", "llm_unavailable"),
        )
        for ((status, file, code) in cases) {
            enqueue(status, fixture(file))
            try {
                runBlocking { client.uploadMaterials(listOf(pdf("a.pdf"))) }
                fail("ApiException expected for $status")
            } catch (e: ApiException) {
                assertEquals(status, e.httpStatus)
                assertEquals(code, e.code)
                assertTrue(e.message.isNotBlank())
            }
        }
    }

    @Test
    fun nonJsonErrorBodyBecomesUnknownCode() {
        enqueue(500, "<html>Internal Server Error</html>")
        try {
            runBlocking { client.listMaterials() }
            fail("ApiException expected")
        } catch (e: ApiException) {
            assertEquals(500, e.httpStatus)
            assertEquals(ApiException.CODE_UNKNOWN, e.code)
        }
    }

    @Test
    fun unreachableServerThrowsNetworkException() {
        server.shutdown()
        try {
            runBlocking { client.listMaterials() }
            fail("NetworkException expected")
        } catch (e: NetworkException) {
            // 서버에 닿지 못한 경우
        }
    }

    private companion object {
        const val DEVICE_ID = "device-test-id"
    }
}
