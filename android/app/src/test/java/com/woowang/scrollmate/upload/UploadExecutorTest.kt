package com.woowang.scrollmate.upload

import com.woowang.scrollmate.data.Material
import com.woowang.scrollmate.data.MaterialDao
import com.woowang.scrollmate.data.MaterialRepository
import com.woowang.scrollmate.data.Question
import com.woowang.scrollmate.data.QuestionDao
import com.woowang.scrollmate.data.SyncState
import com.woowang.scrollmate.net.ApiClient
import com.woowang.scrollmate.net.UploadFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

private class MemoryMaterialDao : MaterialDao {
    val rows = linkedMapOf<String, Material>()

    override fun observeAll(): Flow<List<Material>> = flowOf(rows.values.toList())
    override suspend fun getAll(): List<Material> = rows.values.toList()
    override suspend fun upsertAll(materials: List<Material>) {
        materials.forEach { rows[it.id] = it }
    }
    override suspend fun assignSubject(materialId: String, subjectId: Long?) = Unit
    override suspend fun delete(materialId: String) {
        rows.remove(materialId)
    }
}

private class UnusedQuestionDao : QuestionDao {
    override suspend fun upsertAll(questions: List<Question>) = Unit
    override suspend fun subjectIdsWithQuestions(): List<Long?> = emptyList()
    override suspend fun randomUnanswered(subjectId: Long?): Question? = null
    override suspend fun randomAny(subjectId: Long?): Question? = null
}

private class MemorySyncState : SyncState {
    override var lastQuestionSync: String? = null
}

class UploadExecutorTest {
    private lateinit var server: MockWebServer
    private lateinit var materials: MemoryMaterialDao
    private lateinit var executor: UploadExecutor

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        materials = MemoryMaterialDao()
        val api = ApiClient(server.url("/").toString(), deviceId = { "device-test-id" })
        val repository = MaterialRepository(api, materials, UnusedQuestionDao(), MemorySyncState())
        executor = UploadExecutor(repository)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun fixture(name: String) =
        javaClass.getResource("/fixtures/$name")!!.readText(Charsets.UTF_8)

    private val files = listOf(
        UploadFile("a.pdf", "application/pdf") { ByteArrayInputStream("%PDF-1.4".toByteArray()) },
    )

    private fun respond(code: Int, body: String? = null) {
        server.enqueue(MockResponse().setResponseCode(code).apply { if (body != null) setBody(body) })
    }

    @Test
    fun acceptedUploadSucceedsAndStoresMaterials() = runBlocking {
        respond(202, fixture("upload_accepted.json"))

        val outcome = executor.run(files, subjectId = 4L)

        assertEquals(UploadOutcome.Success, outcome)
        assertTrue(materials.rows.values.all { it.subjectId == 4L })
    }

    @Test
    fun unsupportedFileFailsWithoutRetry() = runBlocking {
        respond(415, fixture("error_unsupported_file.json"))

        assertEquals(UploadOutcome.Failed("unsupported_file"), executor.run(files, null))
        assertTrue(materials.rows.isEmpty())
    }

    @Test
    fun fileTooLargeFailsWithoutRetry() = runBlocking {
        respond(413, fixture("error_file_too_large.json"))

        assertEquals(UploadOutcome.Failed("file_too_large"), executor.run(files, null))
    }

    @Test
    fun tooManyMaterialsIsRetried() = runBlocking {
        respond(429, fixture("error_too_many_materials.json"))

        assertEquals(UploadOutcome.Retry, executor.run(files, null))
    }

    @Test
    fun serverErrorIsRetried() = runBlocking {
        respond(502, fixture("error_llm_unavailable.json"))

        assertEquals(UploadOutcome.Retry, executor.run(files, null))
    }

    @Test
    fun unreachableServerIsRetried() = runBlocking {
        server.shutdown()

        assertEquals(UploadOutcome.Retry, executor.run(files, null))
    }
}
